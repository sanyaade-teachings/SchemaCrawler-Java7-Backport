/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2017, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/

package schemacrawler.schemacrawler;


import java.io.Serializable;
import com.annimon.stream.function.Predicate;

/**
 * Specifies inclusion and exclusion patterns that can be applied to the
 * names, definitions, and other attributes of named objects.
 * <p>
 * The text to check, which could be the fully qualified name of the
 * named object, the definition, or some other attribute of the named
 * object.
 *
 * @author Sualeh Fatehi
 */
@com.annimon.stream.function.FunctionalInterface
public interface InclusionRule
  extends Serializable, Predicate<String>
{

}
