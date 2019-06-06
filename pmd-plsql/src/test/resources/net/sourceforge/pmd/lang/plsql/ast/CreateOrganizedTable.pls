--
-- BSD-style license; for more info see http://pmd.sourceforge.net/license.html
--

CREATE TABLE countries_demo
    ( country_id      CHAR(2)
    , country_name    VARCHAR2(40)
    , currency_name   VARCHAR2(25)
    , currency_symbol VARCHAR2(3)
    , region          VARCHAR2(15) )
    ORGANIZATION INDEX
    STORAGE
     ( INITIAL  4K )
    PCTTHRESHOLD 2
    INCLUDING   country_name
   OVERFLOW
    STORAGE
      ( INITIAL  4K );

create table toys_heap (
  toy_name varchar2(100)
) organization heap;
