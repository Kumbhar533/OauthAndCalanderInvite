package springOauth.oauth20.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class authController {

	private final static Log logger = LogFactory.getLog(authController.class);
	private static final String APPLICATION_NAME = "";
	private static HttpTransport httpTransport;
	private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static com.google.api.services.calendar.Calendar client;

	GoogleClientSecrets clientSecrets;
	GoogleAuthorizationCodeFlow flow;
	Credential credential;

	@org.springframework.beans.factory.annotation.Value("${google.client.client-id}")
	private String clientId;

	@org.springframework.beans.factory.annotation.Value("${google.client.client-secret}")
	private String clientSecret;

	@org.springframework.beans.factory.annotation.Value("${google.client.redirectUri}")
	private String redirectURI;

	private Set<Event> events = new HashSet<>();

	final DateTime date1 = new DateTime("2017-05-05T16:30:00.000+05:30");
	final DateTime date2 = new DateTime(new Date());

	public void setEvents(Set<Event> events) {
		this.events = events;
	}

	@RequestMapping(value = "/login/google", method = RequestMethod.GET)
	public RedirectView googleConnectionStatus(HttpServletRequest request) throws Exception {
		return new RedirectView(authorize());
	}

	private String authorize() throws Exception {
		AuthorizationCodeRequestUrl authorizationUrl;
		if (flow == null) {
			Details web = new Details();
			web.setClientId(clientId);
			web.setClientSecret(clientSecret);
			clientSecrets = new GoogleClientSecrets().setWeb(web);
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
					Collections.singleton(CalendarScopes.CALENDAR)).build();
		}
		authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectURI);
		System.out.println("cal authorizationUrl->" + authorizationUrl);
		return authorizationUrl.build();
	}

	@RequestMapping(value = "/login/google", method = RequestMethod.GET, params = "code")
	public ResponseEntity<String> oauth2Callback(@RequestParam(value = "code") String code) throws IOException {
		String message = "";

		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
			credential = flow.createAndStoreCredential(response, "userID");

			client = new com.google.api.services.calendar.Calendar.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();

			EventDateTime start = new EventDateTime().setDateTime(new DateTime("2024-06-28T11:00:00-07:00"))
					.setTimeZone("Asia/Kolkata");

			EventDateTime end = new EventDateTime().setDateTime(new DateTime("2024-06-28T11:00:00-08:00"))
					.setTimeZone("Asia/Kolkata");

			List<EventAttendee> attendees = new ArrayList<>();

			// Add attendees to the list
			EventAttendee attendee1 = new EventAttendee().setEmail("nileshkumbhar@nimapinfotech.com")
					.setDisplayName("Attendee One").setResponseStatus("needsAction");
			attendees.add(attendee1);

			Event newEvent = new Event().setSummary("Meeting with Team").setDescription("Meeting discussion")
					.setStart(start).setEnd(end).setAttendees(attendees);

			ConferenceSolutionKey conferenceSolutionKey = new ConferenceSolutionKey().setType("hangoutsMeet");
			CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest()
					.setRequestId(java.util.UUID.randomUUID().toString()); // Generate unique request ID
			ConferenceData conferenceData = new ConferenceData().setCreateRequest(createConferenceRequest);
			newEvent.setConferenceData(conferenceData);

			Event createdEvent = client.events().insert("primary", newEvent).setConferenceDataVersion(1).execute();

			String meetingLink = createdEvent.getHangoutLink();

			System.out.printf("Event created my link: %s\n", meetingLink);
			message = "Meeting created with link: " + meetingLink;
		} catch (Exception e) {
			message = "Error creating meeting: " + e.getMessage();
		}

		return new ResponseEntity<>(message, HttpStatus.OK);
	}

	@GetMapping("/user")
	public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
		return Collections.singletonMap("name", principal.getAttribute("name"));
	}

}
