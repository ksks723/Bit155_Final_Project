package kr.or.ns.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import kr.or.ns.page.PageMaker;
import kr.or.ns.vo.Criteria;

@Controller
@RequestMapping("/manager/")
public class ManagerController {

	
	
	
	
	@RequestMapping("index.do")
	public String indexPage() {
		System.out.println("어드민대문이동");

		return "manager/index"; 
	}
	
	@RequestMapping("board/member_Detail.do")
	public String memberDetailPage() {
		System.out.println("sss");

		return "manager/board/member_Detail"; 
	}

	@RequestMapping("board/member_Management.do")
	public String memberManagementPage(Criteria cri, Model model) {
		System.out.println("어드민 회원관리 테이블페이지이동");
		
//		System.out.println(cri.getPage());
//		PageMaker pageMaker = new PageMaker();
//		pageMaker.setCri(cri);
//		pageMaker.setTotalCount(service.getMemberCount());
//		
//		//DAO받아오기 + 매퍼를 통한 인터페이스 연결
//		System.out.println(cri.getPage());
//		System.out.println(cri.getPageStart());
//		System.out.println(cri.getPerPageNum());
//		List<Map<String,Object>> list = null;
//		list = service.getMemberList(cri);
//		model.addAttribute("list", list); //view까지 전달(forward)
//		model.addAttribute("pageMaker",pageMaker); 
//		
//		System.out.println("미네미네미네미네미네미넴니ㅔ " + pageMaker.isNext());
//		System.out.println(list.toString());
//		System.out.println("컨트롤러2");
		

		return "manager/board/member_Management"; 
	}
	
	@RequestMapping("board/report_Management.do")
	public String reportManagementPage() {
		System.out.println("어드민 회원관리 테이블페이지이동");

		return "manager/board/report_Management"; 
	}
}
