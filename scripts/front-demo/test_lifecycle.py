"""Integration check on local Compose DB. Resets demo data; leaves it installed."""
import pathlib
import subprocess
import sys

HERE = pathlib.Path(__file__).resolve().parent


def sql(query, success=True):
    result = subprocess.run(
        ['docker', 'compose', 'exec', '-T', 'postgres', 'sh', '-c',
         'exec psql -X -At -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'],
        cwd=HERE.parent.parent, input=query, encoding='utf-8', capture_output=True)
    assert (result.returncode == 0) == success, result.stderr
    return result.stdout if success else result.stderr


def action(name, success=True):
    p = subprocess.run([sys.executable, str(HERE / 'demo.py'), name], capture_output=True, encoding='utf-8')
    assert (p.returncode == 0) == success, p.stdout + p.stderr
    print(name, 'OK' if success else 'correctly refused', flush=True)


def fingerprint():
    tables = sql("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename LIKE 'back_%' ORDER BY tablename;").splitlines()
    return {t: sql(f"SELECT count(*),md5(coalesce(string_agg(to_jsonb(t)::text, E'\\n' ORDER BY to_jsonb(t)::text),'')) FROM {t} t;") for t in tables}


action('rollback')
before = fingerprint()
action('apply')
action('verify')
action('apply', success=False)
# A foreign account attached to a demo subject must block the whole rollback.
# The injected record lives in the same failing transaction and is never committed.
query = """BEGIN;
INSERT INTO back_users(email,display_name) VALUES('rollback-guard@example.test','Rollback guard');
INSERT INTO back_teacher_subject_access(subject_id,user_id)
SELECT (SELECT id FROM dev_front_demo.roots WHERE kind='subject' LIMIT 1),
       (SELECT id FROM back_users WHERE email='rollback-guard@example.test');
""" + (HERE / 'rollback.sql').read_text(encoding='utf-8') + 'COMMIT;'
assert 'Real/demo cross-links detected' in sql(query, success=False)
print('cross-link guard OK', flush=True)
action('rollback')
assert before == fingerprint(), 'Existing business rows changed during apply/rollback'
print('all existing business rows preserved (count + full-row hashes)', flush=True)
action('rollback')
action('apply')
action('verify')
print('Lifecycle OK; demo installed', flush=True)
