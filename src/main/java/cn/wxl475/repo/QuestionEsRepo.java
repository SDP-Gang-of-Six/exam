package cn.wxl475.repo;

import cn.wxl475.pojo.Image;
import cn.wxl475.pojo.Question;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface QuestionEsRepo extends ElasticsearchRepository<Question, Long> {
}
