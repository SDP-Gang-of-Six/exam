package cn.wxl475.repo;

import cn.wxl475.pojo.exam.Question;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface QuestionEsRepo extends ElasticsearchRepository<Question, Long> {
}
