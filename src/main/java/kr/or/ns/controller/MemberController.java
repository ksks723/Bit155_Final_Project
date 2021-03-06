package kr.or.ns.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.social.google.connect.GoogleConnectionFactory;
import org.springframework.social.google.connect.GoogleOAuth2Template;
import org.springframework.social.oauth2.GrantType;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;

import kr.or.ns.service.AjaxService;
import kr.or.ns.service.MemberService;
import kr.or.ns.service.MyPageService;
import kr.or.ns.vo.AuthInfo;
import kr.or.ns.vo.Users;

@Controller
@RequestMapping("/member/")
public class MemberController {

	/* NaverLoginBO */
	private NaverLoginBO naverLoginBO;
	private String apiResult = null;

	@Autowired
	private void setNaverLoginBO(NaverLoginBO naverLoginBO) {
		this.naverLoginBO = naverLoginBO;
	}

	/* GoogleLogin */
	@Inject
	private AuthInfo authInfo;

	@Autowired
	private GoogleConnectionFactory googleConnectionFactory;

	@Autowired
	private GoogleOAuth2Template googleOAuth2Template;

	@Autowired
	private OAuth2Parameters googleOAuth2Parameters;

	@Autowired
	MemberService service;

	@Autowired
	AjaxService ajaxservice;

	@Autowired
	MyPageService mypageservice;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	// 일반 계정 로그이느 페이지 이동(시큐리티 페일러 핸들러에서 발생하 에러메시지 출력)
	@RequestMapping(value = "normallogin.do", method = RequestMethod.GET)
	public String loginPage(@RequestParam(value = "errormsg", required = false) Object errormsg,
			HttpServletRequest request) {
		Map<String, Object> map = (Map<String, Object>) RequestContextUtils.getInputFlashMap(request);
		if (map != null) {
			request.setAttribute("errormsgname", (String) map.get("errormsg"));
		}
		return "user/member/login";
	}

	// 로그인 페이지 이동
	@RequestMapping(value = "login.do", method = { RequestMethod.GET, RequestMethod.POST })
	public String socialloginPage(@RequestParam(value = "errormsg", required = false) Object errormsg,
			HttpServletRequest request, HttpSession session, Model model) {

		// 네이버 로그인 인증을 위한 url 생성
		String naverUrl = naverLoginBO.getAuthorizationUrl(session);

		// 구글 로그인 인증을 위한 url 생성
		OAuth2Operations oauthOperations = googleConnectionFactory.getOAuthOperations();
		String googleurl = oauthOperations.buildAuthorizeUrl(GrantType.AUTHORIZATION_CODE, googleOAuth2Parameters);
		// 메인이외의 페이지에서 로그인했을시 해당 페이지로 return시켜주기위한 url
		session.setAttribute("loginPage", request.getRequestURL().toString());

		// 넘어온 페이지
		session.setAttribute("returnUrl", (String) request.getHeader("REFERER"));

		// url 전달
		model.addAttribute("naver_url", naverUrl); // 네이버 url
		model.addAttribute("google_url", googleurl); // 구글 url

		return "user/member/login";
	}

	// 네이버 callback호출 메소드
	@RequestMapping(value = "naverlogin.do", method = { RequestMethod.GET, RequestMethod.POST })
	public String naverLogin(Model model, @RequestParam String code, @RequestParam String state, HttpSession session,
			HttpServletRequest request, RedirectAttributes redirect) throws SQLException, Exception {
		OAuth2AccessToken oauthToken;

		oauthToken = naverLoginBO.getAccessToken(session, code, state);
		// 1. 로그인 사용자 정보를 읽어온다.
		apiResult = naverLoginBO.getUserProfile(oauthToken); // String형식의 json데이터

		// 2. String형식인 apiResult를 json형태로 바꿈
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(apiResult);
		JSONObject jsonObj = (JSONObject) obj;

		// 3. 데이터 파싱
		// Top레벨 단계 _response 파싱
		JSONObject response_obj = (JSONObject) jsonObj.get("response");
		// response의 nickname값 파싱
		String nickname = (String) response_obj.get("nickname");
		String name = (String) response_obj.get("name");
		String email = (String) response_obj.get("email");

		String returnUrl = (String) session.getAttribute("returnUrl");

		// DB에 등록된 이메일인지 확인
		int check = 0;
		// 권한확인
		String enabled = null;
		// 회원가입 후의 권한확인
		int after_enabled = 0;
		check = ajaxservice.idcheck(email);
		if (check == 1) {
			after_enabled = ajaxservice.enabledcheck(email);
		}
		try {
			if (check == 0) {
				model.addAttribute("uemail", email);
				model.addAttribute("snstype", "naver");
				// 가입하지 않은 회원이면 회원가입으로 이동시켜주기
				return "user/member/join";
			} else if (check == 1 && after_enabled == 0) {
				String msg = "접근 권한이 없습니다. 관리자에게 문의해주세요.";
				redirect.addFlashAttribute("errormsg", msg);
				return "redirect:/member/normallogin.do";
			} else if (check == 1 && after_enabled == 1) {
				// 스프링 시큐리티 수동 로그인을 위한 작업//
				// 로그인 세션에 들어갈 권한을 설정
				List<GrantedAuthority> list = new ArrayList<GrantedAuthority>();
				list.add(new SimpleGrantedAuthority("ROLE_USER"));

				SecurityContext sc = SecurityContextHolder.getContext();
				// 아이디, 패스워드, 권한을 설정. 아이디는 Object단위로 넣어도 무방하며
				// 패스워드는 null로 하여도 값이 생성.
				sc.setAuthentication(new UsernamePasswordAuthenticationToken(email, null, list));
				session = request.getSession(true);

				// 위에서 설정한 값을 Spring security에서 사용할 수 있도록 세션에 설정
				session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
				// 스프링 시큐리티 수동 로그인을 위한 작업 끝//

				Users users = new Users();
				users = mypageservice.getUsers(email);
				session = request.getSession();
				session.setAttribute("currentUser", users);
			}
		} catch (Exception e) {
			e.getMessage();
		}
		return "redirect:../user/main.do";
	}

	// 구글 콜백함수
	@RequestMapping(value = "googlelogin.do", method = { RequestMethod.GET, RequestMethod.POST })
	public String googleLogin(Model model, @RequestParam(required = false, defaultValue = "0") String code,
			HttpSession session, HttpServletRequest request, RedirectAttributes redirectAttributes,
			HttpServletResponse response, RedirectAttributes redirect) throws IOException, ClassNotFoundException {

		String Googlecode = request.getParameter("code");

		// RestTemplate을 사용하여 Access Token 및 profile을 요청한다.
		RestTemplate restTemplate = new RestTemplate();
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap();
		parameters.add("code", Googlecode);
		parameters.add("client_id", authInfo.getClientId());
		parameters.add("client_secret", authInfo.getClientSecret());
		parameters.add("redirect_uri", googleOAuth2Parameters.getRedirectUri());
		parameters.add("grant_type", "authorization_code");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(
				parameters, headers);
		ResponseEntity<Map> responseEntity = restTemplate.exchange("https://www.googleapis.com/oauth2/v4/token",
				HttpMethod.POST, requestEntity, Map.class);
		Map<String, Object> responseMap = responseEntity.getBody();

		// id_token 라는 키에 사용자가 정보가 존재
		// 받아온 결과는 JWT (Json Web Token) 형식으로 받아옴
		// Base 64로 인코딩 되어 있으므로 디코딩

		String[] tokens = ((String) responseMap.get("id_token")).split("\\.");
		Base64 base64 = new Base64(true);
		String body = new String(base64.decode(tokens[1]));
		String tokenInfo = new String(Base64.decodeBase64(tokens[1]), "utf-8");

		// Jackson을 사용한 JSON을 자바 Map 형식으로 변환
		ObjectMapper temp = new ObjectMapper();
		Map<String, String> googleUserInfo = temp.readValue(tokenInfo, Map.class);

		String email = googleUserInfo.get("email");
		/* 메인이외의 페이지에서 로그인했을시 해당 페이지로 return시켜주기위한 url */
		String returnUrl = (String) session.getAttribute("returnUrl");

		// DB에 등록된 이메일인지 확인
		int check = 0;
		String enabled = null;
		int after_enabled = 0;
		check = ajaxservice.idcheck(email);
		if (check == 1) {
			after_enabled = ajaxservice.enabledcheck(email);
		}
		try {
			if (check == 0) {
				model.addAttribute("uemail", email);
				model.addAttribute("snstype", "google");
				// 가입하지 않은 회원이면 회원가입으로 이동시켜주기
				return "user/member/join";

			} else if (check == 1 && after_enabled == 0) {
				String msg = "접근 권한이 없습니다. 관리자에게 문의해주세요.";
				redirect.addFlashAttribute("errormsg", msg);
				return "redirect:/member/normallogin.do";
			} else if (check == 1 && after_enabled == 1) {
				// 스프링 시큐리티 수동 로그인을 위한 작업//
				// 로그인 세션에 들어갈 권한을 설정
				List<GrantedAuthority> list = new ArrayList<GrantedAuthority>();
				list.add(new SimpleGrantedAuthority("ROLE_USER"));

				SecurityContext sc = SecurityContextHolder.getContext();
				// 아이디, 패스워드, 권한을 설정. 아이디는 Object단위로 넣어도 무방하며
				// 패스워드는 null로 하여도 값이 생성.
				sc.setAuthentication(new UsernamePasswordAuthenticationToken(email, null, list));
				session = request.getSession(true);

				// 위에서 설정한 값을 Spring security에서 사용할 수 있도록 세션에 설정
				session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
				// 스프링 시큐리티 수동 로그인을 위한 작업 끝//

				Users users = new Users();
				users = mypageservice.getUsers(email);
				session = request.getSession();
				session.setAttribute("currentUser", users);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:../user/main.do";
	}

	// 소셜 회원가입 처음
	@RequestMapping(value = "menujoin.do", method = { RequestMethod.GET, RequestMethod.POST })
	public String menujoin(HttpSession session, Model model) {

		String naverAuthUrl = naverLoginBO.getAuthorizationUrl(session);
		OAuth2Operations oauthOperations = googleConnectionFactory.getOAuthOperations();
		String googleurl = oauthOperations.buildAuthorizeUrl(GrantType.AUTHORIZATION_CODE, googleOAuth2Parameters);

		model.addAttribute("url", naverAuthUrl);
		model.addAttribute("google_url", googleurl);

		return "user/member/menujoin";
	}

	// 소셜회원가입처리
	@RequestMapping(value = "join.do", method = RequestMethod.POST)
	public String socialJoin(@RequestParam(value = "file", required = false) MultipartFile ipload, Users users,
			HttpServletRequest request, HttpSession session) throws SQLException, Exception {

		int loginusers = service.socialjoininsert(users, request);

		return "redirect:/index.do";
	}

	@RequestMapping("normaljoin.do")
	public String joinPage() {

		return "user/member/join";
	}

	// 일반회원가입처리
	@RequestMapping(value = "normaljoin.do", method = RequestMethod.POST)
	public String joininsert(@RequestParam(value = "file", required = false) MultipartFile ipload, Users users,
			HttpServletRequest request) throws IOException {
		users.setUser_pwd(this.bCryptPasswordEncoder.encode(users.getUser_pwd()));

		try {
			service.joininsert(users, request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/index.do"; // /index.htm
	}

	//시큐리티 페일러 핸들러이후 loginFail.do주소 매핑
	@RequestMapping("loginFail.do")
	public String loginFailPage(HttpServletRequest request, RedirectAttributes redirect) {
		Object errormsg = request.getAttribute("errormsgname");
		redirect.addFlashAttribute("errormsg", errormsg);
		return "redirect:/member/normallogin.do";
	}

	//아이디 찾기 페이지 이동
	@RequestMapping("find_Id.do")
	public String findIdPage() {
		return "user/member/find_Id";
	}

	//비밀번호 찾기 페이지 이동
	@RequestMapping("find_Passward.do")
	public String findPasswardPage() {
		return "user/member/find_Passward";
	}

	// 로그아웃
	@RequestMapping(value = "logout.do", method = { RequestMethod.GET, RequestMethod.POST })
	public String logout(HttpSession session) throws IOException {
		session.invalidate();
		return "redirect:../user/main.do";
	}

}