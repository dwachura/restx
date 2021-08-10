package io.dwsoft.restx;

import io.dwsoft.restx.fault.cause.StandardCauseProcessor;
import io.dwsoft.restx.fault.cause.code.CauseCodeProviders;
import io.dwsoft.restx.fault.payload.SingleErrorPayloadGenerator;
import io.dwsoft.restx.fault.response.ResponseGenerator;

import static io.dwsoft.restx.RestXKt.get;
import static io.dwsoft.restx.RestXKt.init;
import static io.dwsoft.restx.RestXKt.of;

public class RestXJavaConfigurationTests {
    public void test() {
        ResponseGenerator.Builder.buildFrom(
                new ResponseGenerator.Builder.Config<RuntimeException>() {{
                    payload(get(na -> SingleErrorPayloadGenerator.Builder.buildFrom(
                            new SingleErrorPayloadGenerator.Builder.Config<RuntimeException>() {{
                                identifiedBy(get(na -> na.type(of(RuntimeException.class))));
                                processedBy(get(na -> StandardCauseProcessor.Builder.buildFrom(
                                        new StandardCauseProcessor.Builder.Config<RuntimeException>() {{
                                            code(get(CauseCodeProviders::sameAsCauseId));
                                            message(get(messageProviders ->
                                                    messageProviders.generatedAs(
                                                            cause -> cause.getContext().getMessage()
                                                    )
                                            ));
                                        }})));
                            }})));
                    status(get(statusProviders -> statusProviders.of(500)));
                }}
        );

        RestX.Companion.<Exception>respondTo(init(responseGeneratorConfig -> {
            responseGeneratorConfig.payload(get(payloadGenerators ->
                    payloadGenerators.error(init(singleErrorPayloadGeneratorConfig -> {
                        singleErrorPayloadGeneratorConfig.identifiedBy(get(causeResolvers ->
                                causeResolvers.type(of(Exception.class)))
                        );
                        singleErrorPayloadGeneratorConfig.processedBy(get(causeProcessors ->
                                causeProcessors.standard(init(causeProcessorConfig -> {
                                    causeProcessorConfig.code(CauseCodeProviders::sameAsCauseId);
                                    causeProcessorConfig.message(causeMessageProviders ->
                                            causeMessageProviders.generatedAs(cause -> cause.getContext().getMessage())
                                    );
                                }))));
                    }))));
            responseGeneratorConfig.status(get(statusProviders -> statusProviders.of(500)));
        }));
    }
}
