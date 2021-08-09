package io.dwsoft.restx;

import io.dwsoft.restx.fault.cause.code.CauseCodeProviders;

import static io.dwsoft.restx.RestXKt.get;
import static io.dwsoft.restx.RestXKt.init;
import static io.dwsoft.restx.RestXKt.of;

public class RestXJavaConfigurationTests {
    public void test() {
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
