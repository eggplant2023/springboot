package capstone.capstone.service;

import capstone.capstone.domain.*;
import capstone.capstone.exception.ResourceNotFoundException;
import capstone.capstone.extendedDomain.PostWithPicture;
import capstone.capstone.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {
    @Autowired
    private ModelService modelService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserMemberRepository userMemberRepository;

    @Autowired
    private PictureRepository pictureRepository;

    @Autowired
    private FileHandler fileHandler;

    @Autowired
    private ImageSourceHandler imageSourceHandler;

    @Autowired
    private VisitListRepository visitListRepository;

    @Autowired
    private LikeListRepository likeListRepository;

    public PostWithPicture PostToPostWithPicture(Post post){
        PostWithPicture postWithPicture = new PostWithPicture(post);

        postWithPicture.setNickname(userMemberRepository.getNicknameByUserNum(post.getUser_num()));
        postWithPicture.setCategory_name(modelService.getCategoryName(post.getModel_name()));
        postWithPicture.setPictureURL(pictureRepository.getPictureLocationByPostNo(post.getPost_num()));
        postWithPicture.setFairPrice(postRepository.findFairPrice(post.getModel_name(), post.getGrade()));
        postWithPicture.setLocation(postRepository.findLa(post.getUser_num()), postRepository.findLo(post.getUser_num()));
        if(userMemberRepository.showProfileImage(post.getUser_num()) == null){
            postWithPicture.setProfile_image("https://capstone-eggplant-bucket.s3.ap-northeast-2.amazonaws.com/profile/default.jpg");
        } else {
            postWithPicture.setProfile_image(userMemberRepository.showProfileImage(post.getUser_num()));
        }

        return postWithPicture;
    }

    public List<PostWithPicture> getAllPosts() throws IOException {
        List<PostWithPicture> allPosts = new ArrayList<PostWithPicture>();

        List<Post> list = postRepository.findAllPosts();
        for(Post post : list) {
            PostWithPicture postWithPicture = PostToPostWithPicture(post);
            allPosts.add(postWithPicture);
        }

        return allPosts;
    }

    public List<PostWithPicture> getHiddenPosts() throws IOException {
        List<PostWithPicture> allPosts = new ArrayList<PostWithPicture>();

        List<Post> list = postRepository.findHiddenPosts();
        for(Post post : list) {
            PostWithPicture postWithPicture = PostToPostWithPicture(post);
            allPosts.add(postWithPicture);
        }

        return allPosts;
    }

    public void hidePost(Integer post_num) {
        postRepository.hidePost(post_num);
    }

    public void exposureHiddenPost(Integer post_num) {
        postRepository.exposureHiddenPost(post_num);
    }

    public void createPost(Post post, List<MultipartFile> files) throws Exception {
        // 해당 이미지가 직접 촬영한 이미지인지 도용한 이미지인지 확인
        String imageSource = imageSourceHandler.detectImageSource(files.get(0));

        if(imageSource == "CAPTURED") {
            System.out.println("해당 이미지는 직접 촬영한 이미지로 추정됩니다.");
            post.setIsCaptured(1);
        } else if(imageSource == "DOWNLOADED") {
            System.out.println("해당 이미지는 인터넷을 통해 다운로드된 이미지로 추정됩니다.");
            post.setIsCaptured(0);
        }
        else {
            System.out.println("해당 이미지는 직접 촬영한 이미지로 추정됩니다.");
            post.setIsCaptured(1);
        }

        postRepository.save(post);

        // Amazon S3에 전달받은 사진들을 업로드하고 해당 사진들의 Url이 담긴 Url 리스트를 반환받아 변수 list에 저장
        List<String> list = fileHandler.saveToS3(files, "images/");

        for(String imageUrl : list) {
            // Picture 객체 생성 후 Picture 리스트에 추가
            Picture picture = Picture.builder()
                    .post_num(post.getPost_num())
                    .picture_location(imageUrl).build();

            pictureRepository.save(picture);
        }
    }

    public void updatePost(Integer post_num, Post post) throws Exception {
        postRepository.updatePost(post_num, post.getModel_name(), post.getGrade(), post.getStatus(), post.getPrice(), post.getPost_title(), post.getPost_content());
    }

    public void deletePost(Integer num) {
        List<String> list = pictureRepository.getPictureLocationByPostNo(num);
        for(String picture_location : list) {
            fileHandler.deleteFromS3(picture_location);
        }

        likeListRepository.deletePost(num);
        visitListRepository.deletePost(num);
        postRepository.deletePost(num);
    }

    public void approvePost(Integer num, String model_name) {
        postRepository.approvePost(num, model_name);
    }

    public void rejectPost(Integer num) {
        List<String> list = pictureRepository.getPictureLocationByPostNo(num);
        for(String picture_location : list) {
            fileHandler.deleteFromS3(picture_location);
        }

        postRepository.rejectPost(num);
    }

    public List<Post> getAllWaitingApprovalPosts() throws IOException {
        return postRepository.findAllWaitingApprovalPosts();
    }

    public PostWithPicture getPost(Integer num) throws IOException {
        PostWithPicture postWithPicture = PostToPostWithPicture(postRepository.findById(num)
                .orElseThrow(() -> new ResourceNotFoundException("Not exist Post Data by no : ["+num+"]")));
        return postWithPicture;
    }

    public List<Post> getCategoryPosts(String category){
        return postRepository.findCategory(category);
    }

    public List<Post> getModelPosts(String model) {
        return postRepository.findModel(model);
    }

    public List<Post> getNamePosts(String type, String name) {
        String text = "%" + name + "%";
        if(type.equals("desc"))
            return postRepository.findIncludeNamed(text);
        else{
            return postRepository.findIncludeNamea(text);
        }
    }

    public String getPost_Name(int post_num) { return postRepository.findName(post_num); }

    public String getPost_Host_info(int post_num) { return postRepository.findHostInfo(post_num);}

    public Location getLocation(int post_num) {
        return new Location(postRepository.findPostLocation_la(post_num),postRepository.findPostLocation_lo(post_num));
    }

    public List<PostWithPicture> getAroundLocation(double lon, double lat, double distance){
        List<Integer> user_id = postRepository.findAroundLocation(lon, lat, distance);
        List<Post> postList = new ArrayList<>();
        List<PostWithPicture> postWithPictures = new ArrayList<>();
        for(int id : user_id){
            postList.addAll(postRepository.findAllUser(id));
        }
        for(Post p: postList){
            postWithPictures.add(PostToPostWithPicture(p));
        }

        return postWithPictures;
    }

    public void setStatusSoldout(int post_num){
        postRepository.setStatusSoldout(post_num);
    }

    public List<PostWithPicture> getPost_Today(){
        List<Post> list = postRepository.getPostToday();
        List<PostWithPicture> postWithPictures = new ArrayList<>();
        for(Post p: list){
            postWithPictures.add(PostToPostWithPicture(p));
        }
        return postWithPictures;
    }

    public List<PostWithPicture> getSellPost(int user_num){
        List<Post> list = postRepository.getSellPost(user_num);
        List<PostWithPicture> postWithPictures = new ArrayList<>();
        for(Post p: list){
            postWithPictures.add(PostToPostWithPicture(p));
        }
        return postWithPictures;
    }
}
