package com.ukefu.webim.util.aop;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ukefu.core.UKDataContext;
import com.ukefu.util.UKTools;
import com.ukefu.util.event.MultiUpdateEvent;
import com.ukefu.webim.service.repository.DbDataRepository;

@Aspect
@Component
public class SyncDatabaseExt {
	
	@Autowired
	private DbDataRepository dbDataRes ;
	/** 
     * 定义拦截规则：拦截org.springframework.data.elasticsearch.repository。 
     */  
    @Pointcut("execution(* org.springframework.data.elasticsearch.repository.*.save(*))")  
    public void syncSaveEsData(){}  
    
    /** 
     * 定义拦截规则：拦截org.springframework.data.elasticsearch.repository。 
     */  
    @Pointcut("execution(* org.springframework.data.elasticsearch.repository.*.delete(*))")  
    public void syncDeleteEsData(){}  
      
    @SuppressWarnings("unchecked")
	@Around("syncSaveEsData()")  
    public Object syncSaveEsData(ProceedingJoinPoint pjp) throws Throwable{  
    	Object[] args  = pjp.getArgs() ;
    	if(args.length == 1){
    		Object data = args[0] ;
    		if(data!=null){
	    		if(data instanceof List){
	    			List<Object> dataList = (List<Object>)data ;
	    			for(Object dbData : dataList){
	    				UKTools.multiupdate(new MultiUpdateEvent<Object>(dbData , dbDataRes, UKDataContext.MultiUpdateType.SAVE.toString()));
	    			}
	    		}else{
	    			UKTools.multiupdate(new MultiUpdateEvent<Object>(data, dbDataRes, UKDataContext.MultiUpdateType.SAVE.toString()));
	    		}
    		}
    	}
        return pjp.proceed();  
    }  
    
    @SuppressWarnings("unchecked")
	@Around("syncDeleteEsData()")  
    public Object syncDeleteEsData(ProceedingJoinPoint pjp) throws Throwable{  
    	Object[] args  = pjp.getArgs() ;
    	if(args.length == 1){
    		Object data = args[0] ;
    		if(data instanceof List){
    			List<Object> dataList = (List<Object>)data ;
    			for(Object dbData : dataList){
    				UKTools.multiupdate(new MultiUpdateEvent<Object>(dbData , dbDataRes, UKDataContext.MultiUpdateType.SAVE.toString()));
    			}
    		}else{
    			UKTools.multiupdate(new MultiUpdateEvent<Object>(data, dbDataRes, UKDataContext.MultiUpdateType.SAVE.toString()));
    		}
    	}
        return pjp.proceed();  
    }  
}
