package proxy.common;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CompletableFutures {

    /**
     * 모든 {@link CompletableFuture}가 완료될 때까지 대기 후 결과값 반환 (예외 포함)
     * @param futures
     * @return
     */
    public static <T> CompletableFuture<List<T>> allAsList(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(
                none -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }
}
