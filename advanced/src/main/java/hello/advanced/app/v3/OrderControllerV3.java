package hello.advanced.app.v3;

import hello.advanced.trace.TraceStatus;
import hello.advanced.trace.hellotrace.HelloTraceV2;
import hello.advanced.trace.logtrace.LogTrace;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/*
    로직)
    Controller -> (id, level=0) -> Service (level++) -> (id, level=1) -> Repository (level++)
    => 이때, controller가 가지고 있는 status는 trace.begin을 통해 시작해서 random한 traceId와 level=0의 값을 가지고 있다.
    => service의 경우 trace.beginSync를 통해 시작하는데, 이때 controller로부터 전달받은 status를 사용하게 된다.
    그리고 여기서 level++이 된 level=1을 가지고 있게 된다.
    => 마찬가지로 repository 역시, service가 전달해준 status에서 beginSync로 인해 level++이 된 level=2의 값을 가지게 된다.

    Repository -> (id, level=2) -> Service -> (id, level=1) -> Controller
    : 이런 식으로 traceId를 다음으로 넘겨주게 된다.
    여기서는 이전에 생성되었던 status 정보가 각 메서드마다 고유하게 존재하고 있기 때문에.
    해당 level 값인 (2, 1, 0)을 바탕으로 addSpace를 통해 찍히게 되는 것이다.

 */

@RestController
@RequiredArgsConstructor
public class OrderControllerV3 {

    private final OrderServiceV3 orderServiceV3;
    private final LogTrace trace;

    TraceStatus status = null;

    @GetMapping("/v3/request")
    public String request(String itemId) {

        try {
            // 컨트롤러 이름 + 메서드 이름
            status = trace.begin("OrderController.request()");
            // orderService의 orderItem 호출 시 traceId를 파라미터로 전달한다.
            orderServiceV3.orderItem(itemId);
            trace.end(status);
            return "ok";
        } catch(Exception e) {
            trace.exception(status, e);
            throw e;
        }
    }
}

/*
- 정상 흐름 -
2022-06-13 15:27:03.751  INFO 11044 --- [nio-8080-exec-1] h.advanced.trace.logtrace.FieldLogTrace  : [41f964df] OrderController.request()
2022-06-13 15:27:03.755  INFO 11044 --- [nio-8080-exec-1] h.advanced.trace.logtrace.FieldLogTrace  : [41f964df] |-->OrderService.orderItem()
2022-06-13 15:27:03.755  INFO 11044 --- [nio-8080-exec-1] h.advanced.trace.logtrace.FieldLogTrace  : [41f964df] | |-->OrderRepository.save()
2022-06-13 15:27:04.762  INFO 11044 --- [nio-8080-exec-1] h.advanced.trace.logtrace.FieldLogTrace  : [41f964df] | |<--OrderRepository.save() time=1006ms
2022-06-13 15:27:04.762  INFO 11044 --- [nio-8080-exec-1] h.advanced.trace.logtrace.FieldLogTrace  : [41f964df] |<--OrderService.orderItem() time=1009ms
2022-06-13 15:27:04.762  INFO 11044 --- [nio-8080-exec-1] h.advanced.trace.logtrace.FieldLogTrace  : [41f964df] OrderController.request() time=1011ms

- 예외 발생 -
2022-06-13 15:27:25.762  INFO 11044 --- [nio-8080-exec-3] h.advanced.trace.logtrace.FieldLogTrace  : [26af955c] OrderController.request()
2022-06-13 15:27:25.762  INFO 11044 --- [nio-8080-exec-3] h.advanced.trace.logtrace.FieldLogTrace  : [26af955c] |-->OrderService.orderItem()
2022-06-13 15:27:25.762  INFO 11044 --- [nio-8080-exec-3] h.advanced.trace.logtrace.FieldLogTrace  : [26af955c] | |-->OrderRepository.save()
2022-06-13 15:27:25.763  INFO 11044 --- [nio-8080-exec-3] h.advanced.trace.logtrace.FieldLogTrace  : [26af955c] | |<X-OrderRepository.save() time=1ms ex=java.lang.IllegalStateException: 예외 발생
2022-06-13 15:27:25.763  INFO 11044 --- [nio-8080-exec-3] h.advanced.trace.logtrace.FieldLogTrace  : [26af955c] |<X-OrderService.orderItem() time=1ms ex=java.lang.IllegalStateException: 예외 발생
2022-06-13 15:27:25.764  INFO 11044 --- [nio-8080-exec-3] h.advanced.trace.logtrace.FieldLogTrace  : [26af955c] OrderController.request() time=2ms ex=java.lang.IllegalStateException: 예외 발생

 */
