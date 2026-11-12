"""Local Docker fixtures, standard library only. Run from any directory."""
import argparse
import pathlib
import subprocess

HERE = pathlib.Path(__file__).resolve().parent


def run(sql):
    result = subprocess.run(
        ['docker', 'compose', 'exec', '-T', 'postgres', 'sh', '-c',
         'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'],
        cwd=HERE.parent.parent, input=sql, encoding='utf-8',
        stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise SystemExit(result.stderr)
    print(result.stdout)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['apply', 'rollback', 'reset', 'status', 'verify'])
    args = parser.parse_args()
    # No host/URL override: this tool only targets this repo's local Compose service.
    sql = "BEGIN; SELECT pg_advisory_xact_lock(172936401); SET LOCAL lock_timeout='10s';\n"
    sql += (HERE / 'setup.sql').read_text(encoding='utf-8')
    actions = ['rollback', 'apply'] if args.action == 'reset' else [args.action]
    for action in actions:
        if action == 'apply':
            sql += "\\set demo_image '" + (HERE / 'shapes.webp').read_bytes().hex() + "'\n"
        sql += (HERE / (action + '.sql')).read_text(encoding='utf-8') + '\n'
        if action == 'apply':
            sql += (HERE / 'verify.sql').read_text(encoding='utf-8') + '\n'
    sql += 'COMMIT;'
    run(sql)


if __name__ == '__main__':
    main()
