package br.com.frevonamesa.frevonamesa.config;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PlanoScheduler {

    @Autowired
    private RestauranteRepository restauranteRepository;

    /**
     * Verifica diariamente se os planos PRO/PREMIUM expiraram, aplicando 5 dias de carência.
     * Roda às 3:00 da manhã (horário do servidor) via expressão cron.
     */
    @Scheduled(cron = "0 0 3 * * *") // Roda diariamente às 3 da manhã
    @Transactional
    public void verificarExpiracaoPlanos() {
        // Define a data de corte: data atual - 5 dias de carência
        LocalDateTime dataCorte = LocalDateTime.now().minusDays(5);

        // Busca restaurantes com planos PRO (DeliveryPro OU SalaoPro)
        List<Restaurante> restaurantesAtivos = restauranteRepository.findAllByIsDeliveryProTrueOrIsSalaoProTrue();

        for (Restaurante restaurante : restaurantesAtivos) {
            // 1. Verifica se a data de expiração existe E se é anterior à data de corte (vencido há mais de 5 dias).
            if (restaurante.getDataExpiracaoPlano() != null &&
                    restaurante.getDataExpiracaoPlano().isBefore(dataCorte)) {

                System.out.println("ALERTA CHURN: Restaurante " + restaurante.getNome() + " expirou em " + restaurante.getDataExpiracaoPlano() + ". Rebaixando para GRATUITO.");

                // 2. Rebaixa o plano e remove a data de expiração
                restaurante.setPlano("GRATUITO");
                restaurante.setDeliveryPro(false);
                restaurante.setSalaoPro(false);
                restaurante.setDataExpiracaoPlano(null);

                restauranteRepository.save(restaurante);
            }
        }
        System.out.println("Scheduler de verificação de planos concluído.");
    }
}