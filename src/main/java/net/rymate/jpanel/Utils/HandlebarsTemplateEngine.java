package net.rymate.jpanel.Utils;

/*
 * Copyright 2014
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original from https://github.com/perwendel/spark-template-engines/
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.github.jknack.handlebars.io.FileTemplateLoader;
import org.eclipse.jetty.io.RuntimeIOException;

import spark.ModelAndView;
import spark.TemplateEngine;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;


/**
 * Renders HTML from Route output using
 * https://github.com/jknack/handlebars.java.
 * Defaults to the 'templates' directory under the resource path.
 */
public class HandlebarsTemplateEngine extends TemplateEngine {

    private File file;
    private Handlebars handlebars;

    /**
     * Constructs a handlebars template engine
     */
    public HandlebarsTemplateEngine() {
        this("/templates");
    }

    /**
     * Constructs a handlebars template engine
     *
     * @param resourceRoot the resource root
     */
    public HandlebarsTemplateEngine(String resourceRoot) {
        TemplateLoader templateLoader = new ClassPathTemplateLoader();
        templateLoader.setPrefix(resourceRoot);
        templateLoader.setSuffix(null);

        handlebars = new Handlebars(templateLoader);
    }

    public HandlebarsTemplateEngine(File template) {
        this.file = template;
        TemplateLoader templateLoader = new FileTemplateLoader(file.getParentFile());
        templateLoader.setSuffix(null);

        handlebars = new Handlebars(templateLoader);
    }

    @Override
    public String render(ModelAndView modelAndView) {
        String viewName = modelAndView.getViewName();
        try {
            if (viewName.equals("login.hbs")) {
                TemplateLoader templateLoader = new ClassPathTemplateLoader();
                templateLoader.setPrefix("/templates");
                templateLoader.setSuffix(null);

                Handlebars renderer = new Handlebars(templateLoader);
                Template template = renderer.compile(viewName);
                return template.apply(modelAndView.getModel());
            }
            Template template = handlebars.compile(viewName);
            return template.apply(modelAndView.getModel());
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}