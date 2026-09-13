package study.data_jpa.repository;

public interface NestedCloseProjections {

    String getUsername();
    TeamInfo getTeam();

    interface TeamInfo {
        String getName();
    }
}
