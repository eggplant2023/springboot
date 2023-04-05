package capstone.capstone.service;

import capstone.capstone.domain.Picture;
import capstone.capstone.domain.PostWithPicture;
import capstone.capstone.domain.Posts;
import capstone.capstone.exception.ResourceNotFoundException;
import capstone.capstone.repository.PictureRepository;
import capstone.capstone.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PictureRepository pictureRepository;
    @Autowired
    private FileHandler fileHandler;

    @Autowired
    private ModelService modelService;

    public Posts createPost(Posts post, List<MultipartFile> files) throws Exception {
        postRepository.save(post);

        // Amazon S3에 전달받은 사진을 업로드하고 해당 사진의 정보가 담긴 Picture 리스트를 반환받아 변수 list에 저장
        List<Picture> list = fileHandler.saveToS3(post.getPost_no(), files);

        List<Picture> pictures = new ArrayList<>();
        for(Picture picture : list) {
            pictures.add(pictureRepository.save(picture));
        }

        return null;
    }

    public List<Posts> getAllPost() throws IOException {
        return postRepository.findAll();
    }

    public PostWithPicture getPost(Integer no) throws IOException {
        PostWithPicture postWithPicture = new PostWithPicture(postRepository.findById(no)
                .orElseThrow(() -> new ResourceNotFoundException("Not exist Post Data by no : ["+no+"]")));

        postWithPicture.setCategory_name(modelService.getCategoryName(postWithPicture.getModel_name()));
        postWithPicture.setPictureURL(pictureRepository.findByPostNo(no));

        return postWithPicture;
    }

    public List<Posts> getDatePost() {
        return postRepository.findDate();
    }

    public List<Posts> getCategoryPosts(String category){
        return postRepository.findCategory(category);
    }

    public List<Posts> getModelPosts(String model) { return postRepository.findModel(model); }

    public List<Posts> getNamePosts(String type, String name) {
        return postRepository.findIncludeName(type, name);
    }

    public String getPost_Name(int post_no) { return postRepository.findName(post_no); }

    public String getPost_Host_info(int post_no) { return postRepository.findHostInfo(post_no);}
}