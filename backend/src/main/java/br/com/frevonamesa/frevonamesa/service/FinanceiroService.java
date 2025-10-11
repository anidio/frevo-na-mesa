package br.com.frevonamesa.frevonamesa.service;

import br.com.frevonamesa.frevonamesa.model.Restaurante;
import br.com.frevonamesa.frevonamesa.repository.RestauranteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinanceiroService {

    @Autowired
    private RestauranteService restauranteService;

    @Autowired
    private RestauranteRepository restauranteRepository;

    private static final int PEDIDOS_POR_PACOTE = 10;
    private static final int LIMITE_MESAS_PRO = 50;

    /**
     * Simula a compra de um pacote Pay-per-Use.
     * Na vida real, isso seria chamado após a confirmação do pagamento pelo gateway.
     */
    @Transactional
    public void comprarPacoteDePedidosExtras() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();

        if (!restaurante.getPlano().equals("GRATUITO")) {
            throw new RuntimeException("Seu plano atual já oferece pedidos ilimitados. Nenhuma compra é necessária.");
        }

        // A lógica de "liberar 10 pedidos" é feita subtraindo 10 do contador atual.
        // O contador só voltará a travar quando atingir 5 (limite original).
        restaurante.setPedidosMesAtual(restaurante.getPedidosMesAtual() - PEDIDOS_POR_PACOTE);

        restauranteRepository.save(restaurante);
    }

    /**
     * Simula o upgrade para o Plano Delivery PRO.
     */
    @Transactional
    public void upgradeParaDeliveryPro() {
        Restaurante restaurante = restauranteService.getRestauranteLogado();

        if (restaurante.getPlano().equals("DELIVERY_PRO") || restaurante.getPlano().equals("PREMIUM")) {
            throw new RuntimeException("Você já está no Plano PRO ou Premium.");
        }

        restaurante.setPlano("DELIVERY_PRO");
        restaurante.setPedidosMesAtual(0);
        restaurante.setLimiteMesas(LIMITE_MESAS_PRO);

        restauranteRepository.save(restaurante);
    }
}