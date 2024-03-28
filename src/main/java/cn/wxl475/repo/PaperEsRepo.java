package cn.wxl475.repo;

import cn.wxl475.pojo.exam.Paper;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PaperEsRepo extends ElasticsearchRepository<Paper, Long> {
}
