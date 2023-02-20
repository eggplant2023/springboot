package capstone.capstone.repository;

import capstone.capstone.domain.Posts;
import org.springframework.data.jpa.repository.JpaRepository;

//간단한 CRUD기능은 JpaRepository를 상속하는 것으로 구현이 가능하다
public interface PostRepository extends JpaRepository<Posts, Integer> {

}