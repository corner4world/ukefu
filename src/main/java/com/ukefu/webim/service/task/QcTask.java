package com.ukefu.webim.service.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.ukefu.core.UKDataContext;
import com.ukefu.util.UKTools;
import com.ukefu.webim.service.es.WorkOrdersRepository;
import com.ukefu.webim.service.repository.AgentServiceRepository;
import com.ukefu.webim.service.repository.AgentUserTaskRepository;
import com.ukefu.webim.service.repository.JobDetailRepository;
import com.ukefu.webim.service.repository.OnlineUserRepository;
import com.ukefu.webim.service.repository.QualityConfigRepository;
import com.ukefu.webim.service.repository.QualityMissionHisRepository;
import com.ukefu.webim.service.repository.QualityResultRepository;
import com.ukefu.webim.service.repository.StatusEventRepository;
import com.ukefu.webim.web.model.AgentService;
import com.ukefu.webim.web.model.QualityConfig;
import com.ukefu.webim.web.model.QualityMissionHis;
import com.ukefu.webim.web.model.QualityResult;
import com.ukefu.webim.web.model.StatusEvent;
import com.ukefu.webim.web.model.WorkOrders;

@Configuration
@EnableScheduling
public class QcTask {
	
	@Autowired
	private AgentUserTaskRepository agentUserTaskRes ;
	
	@Autowired
	private OnlineUserRepository onlineUserRes ;
	
	@Autowired
	private JobDetailRepository jobDetailRes ;
	
	@Autowired
	private TaskExecutor taskExecutor;
	
	@Autowired
	private QualityResultRepository qualityResultRes ;
	
	@Autowired
	private AgentServiceRepository agentServiceRes ;
	
	@Autowired
	private StatusEventRepository statusEventRes ;
	
	@Autowired
	private QualityMissionHisRepository qualityMissionHisRes ;
	
	@Autowired
	private WorkOrdersRepository workOrdersRes ;

	@Autowired
	private QualityConfigRepository QualityConfigRes ;
	
	private ElasticsearchTemplate elasticsearchTemplate;
	
	@Autowired
	public void setElasticsearchTemplate(ElasticsearchTemplate elasticsearchTemplate) {
		this.elasticsearchTemplate = elasticsearchTemplate;
    }
	
	@Scheduled(fixedDelay= 43200000) // 每12小时执行一次12*60*60*1000=43200000
    public void task() {
//		System.out.println("qcTask开始执行");
		if(UKDataContext.getContext()!=null && UKDataContext.needRunTask()){	//判断系统是否启动完成，避免 未初始化完成即开始执行 任务
			QualityConfig qcConfig = UKTools.initQualityConfig(UKDataContext.SYSTEM_ORGI) ;
			int archivetime = UKDataContext.QUALITY_ARCHIVE_DEFAULT_DAY;
			int aplarchivetime = UKDataContext.QUALITY_ARCHIVE_DEFAULT_DAY;
			if (qcConfig != null && qcConfig.getArchivetime()!=0) {
				archivetime = qcConfig.getArchivetime() ;
			}
			if (qcConfig != null && qcConfig.getAplarchivetime()!=0) {
				aplarchivetime = qcConfig.getAplarchivetime() ;
			}
			final Date archivedate = UKTools.getLastDay(archivetime); //今天日期减去archivetime
			final Date aplarchivedate = UKTools.getLastDay(aplarchivetime);//今天日期减去aplarchivetime
			int p = 0 ;
			int ps = 100 ;
			List<StatusEvent> statusEventList = null ;
			List<WorkOrders> workOrderList = null;
			List<AgentService> agentServiceList = null;
			List<QualityResult> qualityResultList = null;
			List<QualityMissionHis> qualityMissionHisList = null;
			Page<QualityMissionHis> qualitymissionhisList = null;
			do {
//				System.out.println("qcTask执行，遍历"+p+"次");
				statusEventList = new ArrayList<StatusEvent>() ;
				workOrderList = new ArrayList<WorkOrders>();
				agentServiceList = new ArrayList<AgentService>();
				qualityResultList = new ArrayList<QualityResult>();
				qualityMissionHisList = new ArrayList<QualityMissionHis>();
				qualitymissionhisList = qualityMissionHisRes.findAll(new Specification<QualityMissionHis>(){
					@Override
					public Predicate toPredicate(Root<QualityMissionHis> root, CriteriaQuery<?> query,
							CriteriaBuilder cb) {
						List<Predicate> list = new ArrayList<Predicate>();  
						list.add(cb.equal(root.get("qualitystatus").as(String.class),UKDataContext.QualityStatus.DONE.toString())) ;
						list.add(cb.equal(root.get("orgi").as(String.class),UKDataContext.SYSTEM_ORGI)) ;
						//list.add(cb.and(cb.or(cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class), archivedate)),cb.or(cb.and(cb.equal(root.get("qualityappeal").as(int.class),1),cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class), aplarchivedate)))));
//						list.add(cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class), archivedate)) ;
//						list.add(cb.or(cb.and(cb.equal(root.get("qualityappeal").as(int.class),1),cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class), aplarchivedate)))) ;
//						list.add(
//								cb.and(
//										cb.or(cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class), archivedate)),cb.equal(root.get("qualityappeal").as(int.class),1)));
//						List<Predicate> list2 = new ArrayList<Predicate>();  
//						list2.add(cb.equal(root.get("qualityappeal").as(int.class),1)) ;
//						list2.add(cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class), aplarchivedate)) ;
						//list.add(cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class), aplarchivedate));
						list.add(cb.or(cb.and(cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class), aplarchivedate),cb.equal(root.get("qualityappeal").as(int.class),1)),cb.lessThanOrEqualTo(root.get("qualitytime").as(Date.class),archivedate)));
						Predicate[] p = new Predicate[list.size()];  
						
					    return cb.and(list.toArray(p));
					}}, new PageRequest(p, ps , Sort.Direction.DESC, "createtime")) ;
				if (qualitymissionhisList.getContent()!=null && qualitymissionhisList.getContent().size()>0) {
					for(QualityMissionHis qualitymissionhis : qualitymissionhisList.getContent()){
						if (UKDataContext.QcFormFilterTypeEnum.CALLEVENT.toString().equals(qualitymissionhis.getQualitytype())) {
							StatusEvent statusEvent = statusEventRes.findById(qualitymissionhis.getDataid()) ;
							if (statusEvent!=null) {
								statusEvent.setQualitystatus(UKDataContext.QualityStatus.ARCHIVE.toString());
								statusEventList.add(statusEvent) ;
							}
						}else if (UKDataContext.QcFormFilterTypeEnum.WORKORDERS.toString().equals(qualitymissionhis.getQualitytype())) {
							BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
							boolQueryBuilder.must(QueryBuilders.termQuery("orgi", UKDataContext.SYSTEM_ORGI) );
							boolQueryBuilder.must(QueryBuilders.termQuery("id", qualitymissionhis.getDataid()) );
							List<WorkOrders> woList = workOrdersRes.findByOrgiAndQualitydisorgan(boolQueryBuilder) ;
							if (woList != null && woList.size()>0) {
								WorkOrders workOrders = woList.get(0) ;
								workOrders.setQualitystatus(UKDataContext.QualityStatus.ARCHIVE.toString());
								workOrderList.add(workOrders) ;
							}
						}else if (UKDataContext.QcFormFilterTypeEnum.AGENTSERVICE.toString().equals(qualitymissionhis.getQualitytype())) {
							AgentService agentService = agentServiceRes.findByIdAndOrgi(qualitymissionhis.getDataid(), UKDataContext.SYSTEM_ORGI) ;
							if (agentService != null) {
								agentService.setQualitystatus(UKDataContext.QualityStatus.ARCHIVE.toString());
								agentServiceList.add(agentService) ;
							}
						}
						QualityResult qualityResult = qualityResultRes.findByDataidAndOrgi(qualitymissionhis.getDataid(), UKDataContext.SYSTEM_ORGI) ;
						if (qualityResult != null) {
							qualityResult.setStatus(UKDataContext.QualityStatus.ARCHIVE.toString());
							qualityResultList.add(qualityResult);
						}
						qualitymissionhis.setQualitystatus(UKDataContext.QualityStatus.ARCHIVE.toString());
						qualityMissionHisList.add(qualitymissionhis) ;
					}
					if (statusEventList != null && statusEventList.size()>0) {
						statusEventRes.save(statusEventList) ;
					}
					
					BulkRequestBuilder bulkRequest = elasticsearchTemplate.getClient().prepareBulk();  
					Map<String, String> dataMap = new HashMap<String, String>();
				    if(workOrderList != null && workOrderList.size()>0) {
					    for(WorkOrders workOrders : workOrderList){  
					    	dataMap.put("qualitystatus", workOrders.getQualitystatus()) ;
					        bulkRequest.add(elasticsearchTemplate.getClient().prepareUpdate("uckefu", "uk_workorders", workOrders.getId()).setDoc(dataMap));  
					        dataMap.clear();
					    }  
					    bulkRequest.get();  
					    workOrdersRes.save(workOrderList) ;
				    }
				    
					if (agentServiceList != null && agentServiceList.size()>0) {
						agentServiceRes.save(agentServiceList) ;
					}
					if (qualityMissionHisList != null && qualityMissionHisList.size()>0) {
						qualityMissionHisRes.save(qualityMissionHisList) ;
					}
					if (qualityResultList != null && qualityResultList.size()>0) {
						qualityResultRes.save(qualityResultList) ;
					}
					
					
				}
				p++;
				
			} while (qualitymissionhisList.getContent()!=null && qualitymissionhisList.getContent().size() == ps);
		}
	}
}
