/*
 * Copyright (c) 2022-2023.  Agency for Digital Government (DIGG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.ca.signservice.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.ca.signservice.configuration.ServicePortConstraints;
import se.swedenconnect.ca.service.base.configuration.service.HtmlServiceInfo;

/**
 * Web controller for the main service page. This page is typically a simple static information page that can be
 * accessed to se that the service is up and running. Access to this page is typically restricted to an internal service
 * port.
 */
@Slf4j
@Controller
public class HeadlessCAMainController {

  private final ServicePortConstraints servicePortConstraints;

  private final HtmlServiceInfo htmlServiceInfo;

  @Value("${ca-service.config.bootstrap-css}")
  String bootstrapCss;

  @Autowired
  public HeadlessCAMainController(final HtmlServiceInfo htmlServiceInfo,
      final ServicePortConstraints servicePortConstraints) {
    this.servicePortConstraints = servicePortConstraints;
    this.htmlServiceInfo = htmlServiceInfo;
  }

  @RequestMapping("/main")
  public String mainPageRedirect(final HttpServletRequest servletRequest) {
    // Enforce port restrictions
    try {
      this.servicePortConstraints.validateRequestPort(servletRequest);
    }
    catch (final IOException ex) {
      log.debug("Request violates port restrictions - redirect to no-found");
      return "redirect:not-found";
    }
    return "redirect:/";
  }

  @RequestMapping("/")
  public String mainPage(final HttpServletRequest servletRequest, final Model model) {
    // Enforce port restrictions
    try {
      this.servicePortConstraints.validateRequestPort(servletRequest);
    }
    catch (final IOException ex) {
      log.debug("Request violates port restrictions - redirect to no-found");
      return "redirect:not-found";
    }

    model.addAttribute("htmlInfo", this.htmlServiceInfo);

    return "main-page";
  }
}
