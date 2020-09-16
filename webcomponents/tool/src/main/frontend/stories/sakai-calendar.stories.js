import { html } from 'lit-html';
import { unsafeHTML } from 'lit-html/directives/unsafe-html';
import fetchMock from "fetch-mock";
import { withA11y } from "@storybook/addon-a11y";
import { styles } from "./styles/sakai-styles.js";
import { calendarI18n } from "./i18n/calendar-i18n.js";
import { datepickerI18n } from "./i18n/datepicker-i18n.js";
import { calendarData } from "./data/calendar-data.js";

import '../js/calendar/sakai-calendar.js';
import '@lion/calendar';

export default {
  title: 'Sakai Calendar',
  decorators: [withA11y, (storyFn) => {
    parent.portal = {locale: "en-GB"};
    const baseUrl = "/sakai-ws/rest/i18n/getI18nProperties?locale=en-GB&resourceclass=org.sakaiproject.i18n.InternationalizedMessages&resourcebundle=";
    const calendarI18nUrl = `${baseUrl}calendar`;
    const datepickerI18nUrl = `${baseUrl}date-picker-wc`;
    fetchMock
      .get(calendarI18nUrl, calendarI18n, {overwriteRoutes: true})
      .get(datepickerI18nUrl, datepickerI18n, {overwriteRoutes: true})
      .get(/api\/users\/.*\/calendar/, calendarData, {overwriteRoutes: true})
      .get("*", 500, {overwriteRoutes: true});
    return storyFn();
  }],
};

export const BasicDisplay = () => {

    //<lion-calendar class="demo-calendar"></lion-calendar>
  return html`
    ${unsafeHTML(styles)}
    <sakai-calendar user-id="adrian"></sakai-calendar>
  `;
};