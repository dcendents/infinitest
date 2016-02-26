/*
 * Infinitest, a Continuous Test Runner.
 *
 * Copyright (C) 2010-2013
 * "Ben Rady" <benrady@gmail.com>,
 * "Rod Coffin" <rfciii@gmail.com>,
 * "Ryan Breidenbach" <ryan.breidenbach@gmail.com>
 * "David Gageot" <david@gageot.net>, et al.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.infinitest.mapping;

import static java.util.logging.Level.*;
import static org.apache.commons.lang.StringUtils.*;
import static org.infinitest.util.InfinitestUtils.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.infinitest.parser.*;

import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.io.*;

public class RegexResourceMapping implements ResourceMapping {
	private final File file;
	private final Map<Pattern, List<Pattern>> resourceMappings = Maps.newHashMap();

	public RegexResourceMapping(File file) {
		this.file = file;
		if (!file.exists()) {
			log(INFO, "Resource Mapping file " + file + " does not exist.");
		}
		updateResourceMappingList();
	}

	@Override
	public Set<JavaClass> getTests(Collection<File> changedFiles, ClassFileIndex index) {
		Set<JavaClass> addedTests = Sets.newHashSet();

		for (File changedFile : changedFiles) {
			for (Map.Entry<Pattern, List<Pattern>> entry : resourceMappings.entrySet()) {
				if (entry.getKey().matcher(changedFile.getPath()).matches()) {
					addTests(addedTests, index, entry.getValue());
				}
			}
		}

		addedTests.addAll(index.findChangedParents(addedTests));

		return addedTests;
	}

	private void addTests(Set<JavaClass> addedTests, ClassFileIndex index, List<Pattern> patterns) {
		for (Pattern pattern : patterns) {
			addTests(addedTests, index, pattern);
		}
	}

	private void addTests(Set<JavaClass> addedTests, ClassFileIndex index, Pattern pattern) {
		for (String clazz : index.getIndexedClasses()) {
			if (pattern.matcher(clazz).matches()) {
				addedTests.add(index.findJavaClass(clazz));
			}
		}
	}

	@Override
	public void updateResourceMappingList() {
		resourceMappings.clear();

		if (file.exists()) {
			readMappingFile();
		}
	}

	private void readMappingFile() {
		try {
			for (String line : Files.readLines(file, Charsets.UTF_8)) {
				if (isValidMapping(line)) {
					int index = line.indexOf("=");

					Pattern resource = Pattern.compile(line.substring(0, index));
					List<Pattern> mappings = Lists.newArrayList();
					for (String mapping : line.substring(index + 1).split(",")) {
						mappings.add(Pattern.compile(mapping));
					}

					resourceMappings.put(resource, mappings);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Something horrible happened to the filter file", e);
		}
	}

	private boolean isValidMapping(String line) {
		return !isBlank(line) && !line.startsWith("!") && !line.startsWith("#") && (line.indexOf("=") > 0);
	}
}
