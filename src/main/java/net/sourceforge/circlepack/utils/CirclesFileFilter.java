// <editor-fold defaultstate="collapsed" desc="License">
/*
 * The MIT License
 *
 * Copyright 2012 Jan Moulis and Kamil Rendl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
// </editor-fold>

package net.sourceforge.circlepack.utils;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * File filter for CirclePack program. This filter accepted only files with extensions you add to this filter. Extensions means, when one type of file can have more than one extension (e.g. txt, TXT).
 * 
 * @author Jan Moulis and Kamil Rendl
 * 
 */

public class CirclesFileFilter extends FileFilter {
	/** description of file type */
	private String description;
	/** extensions of file type */
	private String[] extensions;

	/**
	 * Constructor of file filter.
	 * 
	 * @param description
	 *            description of file type
	 * @param extension
	 *            extension of file type
	 */
	public CirclesFileFilter(String description, String extension) {
		this(description, new String[] { extension });
	}

	/**
	 * Constructor of file filter.
	 * 
	 * @param description
	 *            description of file type
	 * @param extensions
	 *            extension of file type
	 */
	public CirclesFileFilter(String description, String[] extensions) {
		if (description == null) {
			this.description = extensions[0];
		} else {
			this.description = description;
		}
		this.extensions = (String[]) extensions.clone();
		toLower(this.extensions);
	}

	/**
	 * Changes all letters in extensions to lowercase.
	 * 
	 * @param array
	 *            array of extensions
	 */
	private void toLower(String array[]) {
		for (int i = 0, n = array.length; i < n; i++) {
			array[i] = array[i].toLowerCase();
		}
	}

	/**
	 * Gets description of file filter.
	 * 
	 * @return description of file filter
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets extension of file filter.
	 * 
	 * @return extension
	 */
	public String getExtension() {
		return extensions[0];
	}

	/**
	 * Gets true, when the file has extension, which is contained in file filter.
	 * 
	 * @param file
	 *            tested file
	 */
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		} else {
			String path = file.getAbsolutePath().toLowerCase();
			for (int i = 0, n = extensions.length; i < n; i++) {
				String extension = extensions[i];
				if ((path.endsWith(extension) && (path.charAt(path.length() - extension.length() - 1)) == '.')) {
					return true;
				}
			}
		}
		return false;
	}
}
