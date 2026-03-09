package com.aditya.aigateway.service;

import com.aditya.aigateway.model.GatewayRequest;
import com.aditya.aigateway.model.GatewayResponse;
import com.aditya.aigateway.provider.AiProvider;
import com.aditya.aigateway.router.ProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayService {
    private final ProviderRouter router;

    public GatewayResponse process(GatewayRequest request){
        //model
        String modelName = router.resolveModel(request);
        //find adapter
        AiProvider provider = router.route(modelName);


        log.info("Routing request | tier={} task={} → model={} provider={}",
                request.getTier(), request.getTask(),
                modelName, provider.getProviderName());

        // 3. Make the actual API call
        GatewayResponse response = provider.call(request, modelName);

        // 4. Attach tier info to response
        response.setTier(request.getTier());

        return response;


    }
}
