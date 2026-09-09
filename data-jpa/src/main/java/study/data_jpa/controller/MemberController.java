package study.data_jpa.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import study.data_jpa.dto.MemberDto;
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

    /*
        Web 확장 - 페이징과 정렬
        - 스프링 데이터가 제공하는 페이징과 정렬 기능을 스프링 MVC에서 편리하게 사용할 수 있다.
        - 파라미터로 Pageable을 받을 수 있다.
        - Pageable은 인터페이스, 실제는 org.springframework.data.domain.PageRequest 객체 생성
        요청 파라미터
        - 예) /members?page=0&size=5&sort=id,desc&sort-username,desc
        - page: 현재 페이지, 0부터 시작
        - size: 한 페이지에 노출할 데이터 건수
        - sort: 정렬 조건을 정의, 기본 형식: sort=정렬속성[,정렬속성...][,asc|desc]
            정렬방향 기본이 asc이므로 생략 가능, 속성마다 정렬 방향을 다르게 적용하려면 sort 파리미터를 여러 번 전달

        [글로벌 설정 방법]
        spring.data.web.pageable.default-page-size=20 /# 기본 페이지 사이즈/
        spring.data.web.pageable.max-page-size=2000 /# 최대 페이지 사이즈/

        [개별 설정 방법]
        @PageableDefault 어노테이션을 사용

        [Page를 1부터 시작하기]
        - 스프링 데이터는 Page를 0부터 시작한다.
        - 만약 1부터 시작하려면 아래의 2가지 방법이 존재
        1. Pageable, Page를 파리미터와 응답 값으로 사용히지 않고, 직접 클래스를 만들어서 처리한다.
            그리고 직접 PageRequest(Pageable 구현체)를 생성해서 리포지토리에 넘긴다. 물론 응답값도 Page 대신에 직접 만들어서 제공해야 한다.
        2. spring.data.web.pageable.one-indexed-parameters 를 true로 설정한다. 그런데 이 방법은 web에서 page 파라미터를 -1 처리 할 뿐이다.
            - 위 설정을 적용하면 클라이언트는 page 파라미터를 1부터 전달할 수 있다.
                예)
                    page=1 → 내부적으로는 page=0으로 변환되어 첫 번째 페이지 조회
                    page=2 → 내부적으로는 page=1로 변환되어 두 번째 페이지 조회
            - 즉, 이 설정은 요청 파라미터의 page 값을 내부적으로 1 감소시켜 Spring Data의 0-based 페이지 번호 체계에 맞춰주는 기능이다.
            - 하지만 Spring Data 내부의 Pageable/Page 자체는 여전히 0부터 시작하는 페이지 번호 체계를 사용한다.
            - 따라서 page=1로 첫 번째 페이지를 요청하더라도, 반환된 Page 객체의 getNumber() 값은 0이다.
            - 따라서 요청은 1-based로 사용할 수 있지만, Page 객체를 그대로 응답하면 응답의 페이지 번호는 0-based로 노출되는 불일치가 발생할 수 있다.
            - API 요청과 응답 모두 페이지 번호를 1부터 사용하고 싶다면 Page를 그대로 반환하지 않고 응답 DTO를 만들어
                Page.getNumber() + 1 값을 반환하는 방식으로 처리하는 것이 좋다.
    */
    @GetMapping("/members")
    public Page<MemberDto> list(@PageableDefault(size = 5, sort = "username", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);
        Page<MemberDto> map = page.map(member -> new MemberDto(member));
        return map;
    }

    @GetMapping("/members2")
    public Page<MemberDto> list2(@PageableDefault(size = 5, sort = "username", direction = Sort.Direction.DESC) Pageable pageable) {
        PageRequest request = PageRequest.of(1, 2);

        Page<Member> page = memberRepository.findAll(request);
        Page<MemberDto> map = page.map(member -> new MemberDto(member));
        return map;
    }

    @PostConstruct
    public void init() {
        for (int i = 0; i < 100; i++) {
            memberRepository.save(new Member("user" + i, i));
        }
    }

}
