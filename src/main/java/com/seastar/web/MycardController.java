package com.seastar.web;

import com.seastar.service.MycardPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by wjl on 2016/8/26.
 */
@Controller
public class MycardController {

    @Autowired
    private MycardPayService mycardPayService;

    @RequestMapping(value = "/mycard/notify", method = RequestMethod.POST)
    @ResponseBody
    public String onMycardNotify(@RequestParam(value = "DATA") String body) {
        return mycardPayService.doNotify(body);
    }

    @RequestMapping(value = "/mycard/cmp", method = RequestMethod.POST)
    @ResponseBody
    public String onMycardDiff(HttpServletRequest request) {
        if (request.getParameter("MyCardTradeNo") != null) {
            return mycardPayService.doDiffByMycardTradeNo(request.getParameter("MyCardTradeNo"));
        } else if (request.getParameter("StartDateTime") != null) {
            return mycardPayService.doDiffByDate(request.getParameter("StartDateTime"), request.getParameter("EndDateTime"));
        }
        return "<BR>";
    }
}
