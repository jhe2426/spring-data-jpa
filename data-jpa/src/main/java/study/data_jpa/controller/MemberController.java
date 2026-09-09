package study.data_jpa.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import study.data_jpa.entity.Member;
import study.data_jpa.repository.MemberRepository;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }
    /*
        도메인 클래스 컨버터
        - HTTP 파라미터로 넘어온 엔티티의 아이디로 엔티티 객체를 찾아서 바인딩
        - HTTP 요청은 회원 id를 받지만 도메인 클래스 컨버터가 중간에 동작해서 회원 엔티티 객체를 반환
        - 도메인 클래스 컨버터도 리파지토리를 사용해서 엔티티를 찾음

        [DomainClassConverter 사용 시 주의]
        - 주의: 도메인 클래스 컨버터로 엔티티를 파라미터로 받으면, 이 엔티티는 단순 조회용으로만 사용해야 한다.
            (트랜잭션이 없는 범위에서 엔티티를 조회했으므로, 엔티티를 변경해도 DB에 반영되지 않는다.)
        - DomainClassConverter는 요청으로 전달된 식별자 값을 이용하여 Spring Data Repository의 findById()를 호출하고 엔티티로 변환한다.
        - 이 조회는 Repository의 읽기 트랜잭션 안에서 수행되지만, Repository 호출이 종료되면 해당 조회 트랜잭션도 종료된다.
        - 따라서 컨트롤러에서 전달받은 엔티티를 변경 작업의 대상으로 사용하면 트랜잭션 경계와 변경 감지 여부가 불명확해질 수 있다.
        - DomainClassConverter로 전달받은 엔티티는 조회 목적으로만 사용하고, 변경이 필요한 경우에는 식별자(id)를 서비스 계층으로 전달한 뒤
             @Transactional 범위에서 엔티티를 다시 조회하여 변경하는 것이 좋다.

        [DomainClassConverter로 조회한 엔티티는 조회 목적으로만 사용하는 이유]
        - 엔티티의 변경 반영 여부가 현재 영속성 컨텍스트와 트랜잭션의 범위에 따라 달라질 수 있기 때문이다.
        - 예를 들어 OSIV가 비활성화되어 조회 이후 엔티티가 준영속 상태가 되었다면 컨트롤러에서 값을 변경해도 Dirty Checking이 발생하지 않아
            DB에 반영되지 않는다.
        - 반면 OSIV가 활성화되어 동일한 EntityManager와 영속성 컨텍스트가 HTTP 요청 동안 유지되는 환경에서는 조회한 엔티티가 계속 관리 상태로 남아 있을 수 있다.
        - 이 상태에서 컨트롤러가 엔티티의 값을 변경한 후, 동일한 EntityManager를 사용하는 쓰기 트랜잭션이 이후 시작되면 트랜잭션 commit 시 flush 과정에서
            해당 변경이 Dirty Checking되어 DB에 반영될 수 있다.
        - 즉, 컨트롤러에서 엔티티를 변경하면 변경 결과가 OSIV나 트랜잭션 경계와 같은 영속성 컨텍스트의 상태에 의존하게 되어 코드의 동작이 명확하지 않게 된다.
        - 따라서 DomainClassConverter로 전달받은 엔티티는 조회 목적으로만 사용하고, 엔티티의 변경은 id를 서비스 계층에 전달한 뒤
            @Transactional 범위에서 엔티티를 조회하고 변경하도록 구현하는 것이 좋다.
    */
    @GetMapping("/members2/{id}")
    public String findMember2(@PathVariable("id") Member member) {
        return member.getUsername();
    }

    @PostConstruct
    public void init() {
        memberRepository.save(new Member("userA"));
    }

}
