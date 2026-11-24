package com.appv4.ai.infrastructure.config;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.appv4.ai.application.port.in.ConnexionModeleUseCase;
import com.appv4.ai.application.port.in.DemanderBesoinUseCase;
import com.appv4.ai.application.port.out.FournisseurIaPort;
import com.appv4.ai.application.port.out.RouteBesoin;
import com.appv4.ai.application.port.out.RouteBesoinPort;
import com.appv4.ai.application.port.out.UsageBesoinPort;
import com.appv4.ai.application.service.ConnexionModeleService;
import com.appv4.ai.application.service.DemanderBesoinService;
import com.appv4.ai.infrastructure.adapter.out.openai.OpenAiFournisseurAdapter;
import com.appv4.ai.infrastructure.adapter.out.persistence.BackAiCallJpaRepository;
import com.appv4.ai.infrastructure.adapter.out.persistence.BackAiConnectionJpaRepository;
import com.appv4.ai.infrastructure.adapter.out.persistence.BackAiTaskRouteJpaRepository;
import com.appv4.ai.infrastructure.adapter.out.persistence.BackAiUsageDayJpaRepository;
import com.appv4.ai.infrastructure.adapter.out.persistence.ConnexionModelePersistenceAdapter;
import com.appv4.ai.infrastructure.adapter.out.persistence.RouteBesoinPersistenceAdapter;
import com.appv4.ai.infrastructure.adapter.out.persistence.UsageBesoinPersistenceAdapter;
import com.appv4.auth.application.port.out.ActiveRolePort;

/**
 * Branche le module IA (BACK-30).
 *
 * <p><b>Business :</b> au premier démarrage, les trois besoins partent sur le modèle de la config.
 * <p><b>Technique :</b> la clé OpenAI vient de l'environnement. Elle n'est pas dans le dépôt.
 * <p><b>Métier / flow :</b> pas de route HTTP ici. Le port de besoin est prêt pour les tickets suivants.
 */
@Configuration
@EnableConfigurationProperties(AiConfiguration.AiProperties.class)
public class AiConfiguration {

	@Bean
	@ConditionalOnBean(BackAiTaskRouteJpaRepository.class)
	RouteBesoinPort routeBesoinPort(BackAiTaskRouteJpaRepository routes) {
		return new RouteBesoinPersistenceAdapter(routes);
	}

	@Bean
	FournisseurIaPort openAiFournisseur(AiProperties properties) {
		return new OpenAiFournisseurAdapter(properties.getOpenaiBaseUrl(), properties.getOpenaiApiKey());
	}

	@Bean
	@ConditionalOnBean(BackAiCallJpaRepository.class)
	UsageBesoinPort usageBesoinPort(BackAiCallJpaRepository calls, BackAiUsageDayJpaRepository days) {
		return new UsageBesoinPersistenceAdapter(calls, days);
	}

	@Bean
	@ConditionalOnBean({BackAiTaskRouteJpaRepository.class, BackAiCallJpaRepository.class})
	DemanderBesoinUseCase demanderBesoinUseCase(
			RouteBesoinPort routes,
			List<FournisseurIaPort> fournisseurs,
			AiProperties properties,
			UsageBesoinPort usage,
			Clock clock) {
		return new DemanderBesoinService(routes, fournisseurs, properties.depart(), usage, clock);
	}

	@Bean
	@ConditionalOnBean(BackAiTaskRouteJpaRepository.class)
	ApplicationRunner poserLesBesoinsDeDepart(RouteBesoinPort routes, AiProperties properties) {
		return args -> {
			if (!routes.toutes().isEmpty()) {
				return;
			}
			for (RouteBesoin route : properties.depart()) {
				routes.enregistrer(route);
			}
		};
	}

	@Bean
	@ConditionalOnBean(BackAiConnectionJpaRepository.class)
	ConnexionModeleUseCase connexionModeleUseCase(
			ActiveRolePort roles,
			BackAiConnectionJpaRepository connexions,
			List<FournisseurIaPort> fournisseurs) {
		return new ConnexionModeleService(roles, new ConnexionModelePersistenceAdapter(connexions), fournisseurs);
	}

	/**
	 * Départ des trois besoins (BACK-30).
	 *
	 * <p><b>Business :</b> le même modèle sert les trois jusqu'à ce que l'admin en connecte un autre.
	 * <p><b>Technique :</b> {@code openai-api-key} reste vide si la variable n'est pas posée.
	 */
	@ConfigurationProperties(prefix = "appv4.ai")
	public static class AiProperties {

		private String fournisseur = "openai";
		private String modele = "depart";
		private String openaiBaseUrl = "https://api.openai.com/v1";
		private String openaiApiKey = "";

		public List<RouteBesoin> depart() {
			return List.of(
					new RouteBesoin("question-draft", "Proposer des questions", fournisseur, modele, true),
					new RouteBesoin("answer-analysis", "Analyser une réponse", fournisseur, modele, true),
					new RouteBesoin("learner-support", "Encourager un élève", fournisseur, modele, true));
		}

		public String getFournisseur() {
			return fournisseur;
		}

		public void setFournisseur(String fournisseur) {
			this.fournisseur = fournisseur;
		}

		public String getModele() {
			return modele;
		}

		public void setModele(String modele) {
			this.modele = modele;
		}

		public String getOpenaiBaseUrl() {
			return openaiBaseUrl;
		}

		public void setOpenaiBaseUrl(String openaiBaseUrl) {
			this.openaiBaseUrl = openaiBaseUrl;
		}

		public String getOpenaiApiKey() {
			return openaiApiKey;
		}

		public void setOpenaiApiKey(String openaiApiKey) {
			this.openaiApiKey = openaiApiKey;
		}
	}
}
