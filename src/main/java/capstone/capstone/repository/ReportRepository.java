package capstone.capstone.repository;

import capstone.capstone.domain.Report_list;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report_list, Integer> {
    // 해당 신고 게시글 숨김 처리
    @Modifying
    @Transactional
    @Query(value = "UPDATE Post SET status = '숨김' WHERE post_no = (SELECT post_no FROM report_list WHERE report_num = :report_num)"
            + "UPDATE report_list SET status = '처리' WHERE report_num = :report_num",
            nativeQuery = true)
    void hideReportList(@Param("report_num") Integer report_num);

    // 해당 숨김 처리된 게시글 공개 처리
    @Modifying
    @Transactional
    @Query(value = "UPDATE Post SET status = 'S' WHERE post_no = (SELECT post_no FROM report_list WHERE report_num = :report_num)", nativeQuery = true)
    void exposureReportList(@Param("report_num") Integer report_num);

    // 해당 신고 게시글 삭제 처리
    @Modifying
    @Transactional
    @Query(value = "Delete from Post WHERE post_no = (SELECT post_no FROM report_list WHERE report_num = :report_num)"
            + "UPDATE report_list SET status = '처리' WHERE report_num = :report_num",
            nativeQuery = true)
    void deleteReportList(@Param("report_num") Integer report_num);

    // 신고 목록 데이터 리턴
    @Query(value="select * from Report_list", nativeQuery = true)
    List<Report_list> getAllReportList();
}