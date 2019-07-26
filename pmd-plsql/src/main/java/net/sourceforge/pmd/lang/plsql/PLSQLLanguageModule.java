/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.plsql;

import net.sourceforge.pmd.lang.BaseLanguageModule;
import net.sourceforge.pmd.lang.plsql.rule.PLSQLRuleChainVisitor;

/**
 * Created by christoferdutz on 20.09.14.
 */
public class PLSQLLanguageModule extends BaseLanguageModule {

    public static final String NAME = "PLSQL";
    public static final String TERSE_NAME = "plsql";

    public PLSQLLanguageModule() {
        super(NAME, null, TERSE_NAME, PLSQLRuleChainVisitor.class,
                "sql",
                "tab", "vw", // Tables and Views
                "trg",  // Triggers
                "prc", "fnc", // Standalone Procedures and Functions
                "pld", // Oracle*Forms
                "pls", "plh", "plb", // Packages
                "pck", "pks", "pkh", "pkb", // Packages
                "spc", "bdy", // Packages
                "typ", "tyb", // Object Types
                "tps", "tpb", // Object Types
                "syn", // Synonyms
                "seq" // Sequencies
        );
        addVersion("", new PLSQLHandler(), true);
    }
}
