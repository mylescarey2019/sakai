/**
 * Copyright (c) 2003-2021 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.datemanager.api;

import java.util.Locale;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.sakaiproject.datemanager.api.model.DateManagerValidation;
import org.sakaiproject.site.api.Site;

public interface DateManagerService {

	public final String STATE_SITE_ID = "site.instance.id";

	// Global methods
	public String getCurrentUserId();
	public String getCurrentSiteId();
	public Optional<Site> getCurrentSite();
	public Locale getUserLocale();
        public Locale getLocaleForCurrentSiteAndUser();
	public String getMessage(String messageId);
	public boolean currentSiteContainsTool(String commonId);
	public String getToolTitle(String commonId);

	// Assignments methods
	public JSONArray getAssignmentsForContext(String siteId);
	public DateManagerValidation validateAssignments(String siteId, JSONArray assignments) throws Exception;
	public void updateAssignments(DateManagerValidation assignmentsValidation) throws Exception;

	// Assessments methods
	public JSONArray getAssessmentsForContext(String siteId);
	public DateManagerValidation validateAssessments(String siteId, JSONArray assessments) throws Exception;
	public void updateAssessments(DateManagerValidation assessmentsValidation) throws Exception;

	// Gradebook methods
	public JSONArray getGradebookItemsForContext(String siteId);
	public DateManagerValidation validateGradebookItems(String siteId, JSONArray gradebookItems) throws Exception;
	public void updateGradebookItems(DateManagerValidation gradebookItemsValidation) throws Exception;

	// Signup methods
	public JSONArray getSignupMeetingsForContext(String siteId);
	public DateManagerValidation validateSignupMeetings(String siteId, JSONArray signupMeetings) throws Exception;
	public void updateSignupMeetings(DateManagerValidation signupValidation) throws Exception;

	// Resources methods
	public JSONArray getResourcesForContext(String siteId);
	public DateManagerValidation validateResources(String siteId, JSONArray resources) throws Exception;
	public void updateResources(DateManagerValidation resourceValidation) throws Exception;
	public void clearUpdateResourceLocks(DateManagerValidation resourceValidation) throws Exception;

	// Calendar methods
	public JSONArray getCalendarEventsForContext(String siteId);
	public DateManagerValidation validateCalendarEvents(String siteId, JSONArray calendarEvents) throws Exception;
	public void updateCalendarEvents(DateManagerValidation calendarValidation) throws Exception;
	public void clearUpdateCalendarLocks(DateManagerValidation calendarValidation) throws Exception;

	// Forum methods
	public JSONArray getForumsForContext(String siteId);
	public DateManagerValidation validateForums(String siteId, JSONArray forums) throws Exception;
	public void updateForums(DateManagerValidation forumValidation) throws Exception;

	// Announcement methods
	public JSONArray getAnnouncementsForContext(String siteId);
	public DateManagerValidation validateAnnouncements(String siteId, JSONArray announcements) throws Exception;
	public void updateAnnouncements(DateManagerValidation announcementValidation) throws Exception;
	public void clearUpdateAnnouncementLocks(DateManagerValidation announcementValidation) throws Exception;

	// Lessons methods
	public JSONArray getLessonsForContext(String siteId);
	public DateManagerValidation validateLessons(String siteId, JSONArray lessons) throws Exception;
	public void updateLessons(DateManagerValidation lessonsValidation) throws Exception;

	public DateManagerValidation validateTool(String toolId, int idx, String[][] columnsNames, String[] toolColumnsAux);
	public void updateTool(String toolId, DateManagerValidation dateManagerValidation);

	public boolean isChanged(String toolId, String[] columns);
}
