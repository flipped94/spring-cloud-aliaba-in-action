package com.flipped.learn.goodsservice.controller;

import com.flipped.learn.goodsservice.service.async.AsyncTaskManager;
import com.flipped.learn.goodsservice.vo.AsyncTaskInfo;
import com.flipped.learn.servicesdk.goods.GoodsInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <h1>异步任务服务对外提供的 API</h1>
 */
@Api(tags = "商品异步入库服务")
@Slf4j
@RestController
@RequestMapping("/async-goods")
public class AsyncGoodsController {

    @Resource
    private AsyncTaskManager asyncTaskManager;


    @ApiOperation(value = "导入商品", notes = "导入商品进入到商品表", httpMethod = "POST")
    @PostMapping("/import-goods")
    public AsyncTaskInfo importGoods(@RequestBody List<GoodsInfo> goodsInfos) {
        return asyncTaskManager.submit(goodsInfos);
    }

    @ApiOperation(value = "查询状态", notes = "查询异步任务的执行状态", httpMethod = "GET")
    @GetMapping("/task-info")
    public AsyncTaskInfo getTaskInfo(@RequestParam String taskId) {
        return asyncTaskManager.getTaskInfo(taskId);
    }
}
