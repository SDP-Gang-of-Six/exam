package cn.wxl475;

import cn.wxl475.pojo.Paper;
import cn.wxl475.repo.PaperEsRepo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@SpringBootTest(args = "--spring.profiles.active=dev")
@RunWith(SpringRunner.class)
public class PaperEsTest {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private PaperEsRepo paperEsRepo;

    @Test
    public void createPaperIndex(){
        Class<Paper> aClass = Paper.class;
        boolean created = elasticsearchRestTemplate.indexOps(aClass).createWithMapping();
        if(created){
            log.info("创建索引：{}",aClass.getName()+",成功");
        }else {
            log.info("创建索引：{}",aClass.getName()+",失败");
        }
    }

    @Test
    public void deletePaperIndex(){
        Class<Paper> aClass = Paper.class;
        boolean deleted = elasticsearchRestTemplate.indexOps(aClass).delete();
        if(deleted){
            log.info("删除索引：{}",aClass.getName()+",成功");
        }else {
            log.info("删除索引：{}",aClass.getName()+",失败");
        }
    }

    @Test
    public void findAllPaper(){
        paperEsRepo.findAll().forEach(paper -> { log.info(String.valueOf(paper));});
    }

    @Test
    public void deleteAllPaper(){
        paperEsRepo.deleteAll();
    }
}
