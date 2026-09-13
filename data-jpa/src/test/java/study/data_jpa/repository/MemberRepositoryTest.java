package study.data_jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.data_jpa.dto.MemberDto;
import study.data_jpa.entity.Member;
import study.data_jpa.entity.Team;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(value = false)
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;
    @PersistenceContext
    EntityManager em;

    @Autowired
    MemberQueryRepository memberQueryRepository;

    @Test
    public void testMember() throws Exception {
        // given
        System.out.println("memberRepository.getClass() = " + memberRepository.getClass());
        Member member = new Member("memberA");
        Member savedMember = memberRepository.save(member);

        // when
        Member findMember = memberRepository.findById(savedMember.getId()).get();

        // then
        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);

    }

    @Test
    public void basicCRUD() throws Exception {
        // given
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        // when
        Member findMember1 = memberRepository.findById(member1.getId()).get();
        Member findMember2 = memberRepository.findById(member2.getId()).get();

        // then
        // 단건 조회 검증
        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        // 리스트 조회 검증
        List<Member> all = memberRepository.findAll();
        assertThat(all.size()).isEqualTo(2);

        // 카운트 검증
        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        // 삭제 검증
        memberRepository.delete(member1);
        memberRepository.delete(member2);

        long deletedCount = memberRepository.count();
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    public void findByUsernameAndAgeGreaterThan() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("AAA", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);

        // when
        List<Member> result = memberRepository.findByUsernameAndAgeGreaterThan("AAA", 15);

        // then
        assertThat(result.get(0).getUsername()).isEqualTo("AAA");
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void findHelloBy() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("AAA", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);

        // when
        List<Member> helloBy = memberRepository.findHelloBy();

        // then
        assertThat(helloBy.size()).isEqualTo(2);
    }

    @Test
    public void findTop3HelloBy() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("AAA", 20);
        Member member3 = new Member("AAA", 30);
        Member member4 = new Member("AAA", 40);
        memberRepository.save(member1);
        memberRepository.save(member2);
        memberRepository.save(member3);
        memberRepository.save(member4);

        // when
        List<Member> top3HelloBy = memberRepository.findTop3HelloBy();

        // then
        assertThat(top3HelloBy.size()).isEqualTo(3);
    }

    @Test
    public void namedQuery() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);

        // when
        List<Member> result = memberRepository.findByUsername("AAA");
        Member findMember = result.get(0);
        // then
        assertThat(findMember).isEqualTo(member1);
    }

    @Test
    public void testQuery() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);

        // when
        List<Member> result = memberRepository.findMember("AAA", 10);

        // then
        assertThat(result.get(0)).isEqualTo(member1);
    }

    @Test
    public void findUsernameList() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);

        // when
        List<String> usernameList = memberRepository.findUsernameList();
        for (String name : usernameList) {
            System.out.println("name = " + name);
        }
    }

    @Test
    public void findMemberDto() throws Exception {
        // given
        Team team = new Team("teamA");
        teamRepository.save(team);

        Member member1 = new Member("AAA", 10);
        member1.changeTeam(team);
        memberRepository.save(member1);

        // when
        List<MemberDto> usernameList = memberRepository.findMemberDto();
        for (MemberDto memberDto : usernameList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findByNames() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);

        // when
        List<Member> result = memberRepository.findByNames(Arrays.asList("AAA", "BBB"));
        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }

    @Test
    public void returnType() throws Exception {
        // given
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);

        // when
        List<Member> findMember = memberRepository.findListByUsername("AAA");
        Member findMember2 = memberRepository.findMemberByUsername("AAA");
        System.out.println("findMember2 = " + findMember2);
        Optional<Member> findMember3 = memberRepository.findOptionalByUsername("AAA");

    }

    @Test
    public void paging() throws Exception {
        // given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));

        int age = 10;
        // 스프링 JPA는 page를 0부터 시작함
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));

        // when
        Page<Member> page = memberRepository.findByAge(age, pageRequest);

        Page<MemberDto> toMap = page.map(member -> new MemberDto(member.getId(), member.getUsername(), null));

        // then
        List<Member> content = page.getContent();
        long totalCount = page.getTotalElements();

        assertThat(content.size()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();

    }

    @Test
    public void pagingSlice() throws Exception {
        // given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));

        int age = 10;
        // 스프링 JPA는 page를 0부터 시작함
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));

        // when
        Slice<Member> page = memberRepository.findSliceByAge(age, pageRequest);

        // then
        List<Member> content = page.getContent();

        assertThat(content.size()).isEqualTo(3);
//        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getNumber()).isEqualTo(0);
//        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();

    }

    @Test
    public void findByAgeWithCountQuery() throws Exception {
        // given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));

        int age = 10;
        // 스프링 JPA는 page를 0부터 시작함
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));

        // when
        Page<Member> page = memberRepository.findCountQueryByAge(age, pageRequest);

        // then
        List<Member> content = page.getContent();
        long totalCount = page.getTotalElements();

        assertThat(content.size()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();

    }

    @Test
    public void bulkUpdate() {
        // given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 19));
        memberRepository.save(new Member("member3", 20));
        memberRepository.save(new Member("member4", 30));
        memberRepository.save(new Member("member5", 40));

        // when
        int resultCount = memberRepository.bulkAgePlus(20);
//        em.flush();
//        em.clear();

        List<Member> result = memberRepository.findByUsername("member5");
        Member member5 = result.get(0);
        System.out.println("member5 = " + member5);

        // then
        assertThat(resultCount).isEqualTo(3);
    }

    @Test
    public void findMemberLazy() throws Exception {
        // given
        // member1 -> teamA
        // member2 -> teamB
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamB);
        memberRepository.save(member1);
        memberRepository.save(member2);

        em.flush();
        em.clear();

        // when
        List<Member> members = memberRepository.findAll(); // select Member
        for (Member member : members) {
            System.out.println("member = " + member.getUsername());
            System.out.println("member.teamClass = " + member.getTeam().getClass());
            System.out.println("member.team = " + member.getTeam().getName());
        }
        
    }

    @Test
    public void findMemberFetchJoin() throws Exception {
        // given
        // member1 -> teamA
        // member2 -> teamB
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamB);
        memberRepository.save(member1);
        memberRepository.save(member2);

        em.flush();
        em.clear();

        // when
        List<Member> members = memberRepository.findMemberFetchJoin();
        for (Member member : members) {
            System.out.println("member = " + member.getUsername());
            System.out.println("member.teamClass = " + member.getTeam().getClass());
            System.out.println("member.team = " + member.getTeam().getName());
        }

    }

    @Test
    public void entityGraph() throws Exception {
        // given
        // member1 -> teamA
        // member2 -> teamB
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamB);
        memberRepository.save(member1);
        memberRepository.save(member2);

        em.flush();
        em.clear();

        // when
        List<Member> members = memberRepository.findAll();
        for (Member member : members) {
            System.out.println("member = " + member.getUsername());
            System.out.println("member.teamClass = " + member.getTeam().getClass());
            System.out.println("member.team = " + member.getTeam().getName());
        }
    }

    @Test
    public void findEntityGraphByUsername() throws Exception {
        // given
        // member1 -> teamA
        // member2 -> teamB
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member1", 10, teamB);
        memberRepository.save(member1);
        memberRepository.save(member2);

        em.flush();
        em.clear();

        // when
        List<Member> members = memberRepository.findEntityGraphByUsername("member1");
        for (Member member : members) {
            System.out.println("member = " + member.getUsername());
            System.out.println("member.teamClass = " + member.getTeam().getClass());
            System.out.println("member.team = " + member.getTeam().getName());
        }
    }

    @Test
    public void queryHint() {
        // given
        Member member1 = memberRepository.save(new Member("member1", 10));
        memberRepository.save(member1);
        em.flush();
        em.clear();

        // when
        /*
            Member findMember = memberRepository.findById(member1.getId()).get();
            // findMember.changeUsername("member2");
            - JPA는 영속 상태의 엔티티에 대해 변경 감지(Dirty Checking)를 수행하기 위해 현재 관리 중인 엔티티의 상태뿐만 아니라,
                엔티티를 조회한 시점의 동일한 상태값을 스냅샷으로 별도 보관한다.
            - 즉, 하나의 엔티티를 조회하더라도 영속성 컨텍스트는 현재 엔티티의 상태와 비교 기준이 되는 스냅샷이라는
                두 개의 상태 정보를 메모리에 유지하게 된다.
            - 따라서 조회 후 실제로 엔티티의 값을 변경하지 않더라도, 일반적인 영속 엔티티 조회에서는 현재 엔티티 상태와 비교하기 위한
                최초 조회 시점의 스냅샷이 영속성 컨텍스트 내부에 유지된다.
            - 이후 엔티티의 필드 값이 변경되면 현재 엔티티의 상태가 변경되고, flush 시점에 현재 상태와 최초 조회 시점의 스냅샷을 비교하여
                변경이 감지된 경우 UPDATE SQL을 실행한다.
            - 즉, 단순 조회 목적으로 엔티티를 가져온 경우에도 기본적으로 Dirty Checking을 위한 스냅샷 관리 비용이 발생한다.
        */
        Member findMember = memberRepository.findReadOnlyByUsername("member1");
        findMember.changeUsername("member2");

        em.flush();
    }

    @Test
    public void lock() throws Exception {
        // given
        Member member1 = memberRepository.save(new Member("member1", 10));
        memberRepository.save(member1);
        em.flush();
        em.clear();

        // when
        List<Member> result = memberRepository.findLockByUsername("member1");
    }

    @Test
    public void callCustom() {
        List<Member> result = memberRepository.findMemberCustom();
    }

    @Test
    public void specBasic() {
        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 0, teamA);
        Member member2 = new Member("member2", 0, teamA);
        em.persist(member1);
        em.persist(member2);

        em.flush();
        em.clear();

        // when
        Specification<Member> spec = MemberSpec.username("member1").and(MemberSpec.teamName("teamA"));
        List<Member> result = memberRepository.findAll(spec);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void queryByExample() {
        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 0, teamA);
        Member member2 = new Member("member2", 0, teamA);
        em.persist(member1);
        em.persist(member2);

        em.flush();
        em.clear();

        // when
        // Probe
        Member member = new Member("member1");
        Team team = new Team("teamA");
        member.changeTeam(team);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreCase("age");

        Example<Member> example = Example.of(member, matcher);

        List<Member> result = memberRepository.findAll(example);

        // then
        assertThat(result.get(0).getUsername()).isEqualTo("member1");

    }

    /*
        Projections: 엔티티 대신에 DTO를 편리하게 조회할 때 사용
            전체 엔티티가 아니라 회원 이름만 조회하고 싶은 이러한 경우에 유용하게 사용
    */
    @Test
    public void projections() {
        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 0, teamA);
        Member member2 = new Member("member2", 0, teamA);
        em.persist(member1);
        em.persist(member2);

        em.flush();
        em.clear();

        // when
        List<UsernameOnly> result = memberRepository.findProjectionsByUsername("member1");

        // then
        for (UsernameOnly usernameOnly : result) {
            System.out.println("usernameOnly = " + usernameOnly.getUsername());
        }
    }

    @Test
    public void projections2() {
        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 0, teamA);
        Member member2 = new Member("member2", 0, teamA);
        em.persist(member1);
        em.persist(member2);

        em.flush();
        em.clear();

        // when
        List<UsernameOnlyDto> result = memberRepository.findProjections2ByUsername("member1", UsernameOnlyDto.class);

        // then
        for (UsernameOnlyDto usernameOnly : result) {
            System.out.println("usernameOnly = " + usernameOnly.getUsername());
        }
    }

    /*
        Projections 주의
        - 프로젝션 대상이 root 엔티티이면, JPQL SELECT절 최적화 가능
        - 프로젝션 대상이 ROOT가 아니면
            - LEFT OUTER JOIN으로 처리
            - 모든 필드를 SELECT해서 엔티티로 조회한 다음에 계산을 진행

        정리
        - 프로젝션 대상이 root 엔티티이면 해당 엔티티의 특정 필드만 결과로 가져올 때 사용하기 유용하다.
        - 프로젝션 대상이 root 엔티티를 넘어가면 JPQL SELECT 최적화가 안 된다.
        - 실무의 복잡한 쿼리를 해결하기에는 한계가 있다.
        - 실무에서는 단순할 때에만 사용하고, 조금만 복잡해지만 QueryDSL을 사용한다.
    */
    @Test
    public void projections3() {
        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 0, teamA);
        Member member2 = new Member("member2", 0, teamA);
        em.persist(member1);
        em.persist(member2);

        em.flush();
        em.clear();

        // when
        List<NestedCloseProjections> result = memberRepository.findProjections2ByUsername("member1", NestedCloseProjections.class);

        // then
        for (NestedCloseProjections nestedCloseProjections : result) {
            String username = nestedCloseProjections.getUsername();
            System.out.println("username = " + username);
            String teamName = nestedCloseProjections.getTeam().getName();
            System.out.println("teamName = " + teamName);
        }
    }

    @Test
    public void nativeQuery() {
        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 0, teamA);
        Member member2 = new Member("member2", 0, teamA);
        em.persist(member1);
        em.persist(member2);

        em.flush();
        em.clear();

        // when
        Member result = memberRepository.findByNativeQuery("member1");

        // then
        System.out.println("result = " + result);
    }

    @Test
    public void nativeQueryProjection() {
        // given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member member1 = new Member("member1", 0, teamA);
        Member member2 = new Member("member2", 0, teamA);
        em.persist(member1);
        em.persist(member2);

        em.flush();
        em.clear();

        // when
        Page<MemberProjection> result = memberRepository.findByNativeProjection(PageRequest.of(0, 10));

        // then
        List<MemberProjection> content = result.getContent();
        for (MemberProjection memberProjection : content) {
            System.out.println("memberProjection.getUsername = " + memberProjection.getUsername());
            System.out.println("memberProjection.getTeamName = " + memberProjection.getTeamName());
        }
    }
}