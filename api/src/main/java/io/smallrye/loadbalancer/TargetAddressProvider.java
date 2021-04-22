package io.smallrye.loadbalancer;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface TargetAddressProvider {
    CompletionStage<List<TargetAddress>> getAddressList();
}
