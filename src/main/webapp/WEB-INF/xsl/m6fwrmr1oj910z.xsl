<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:variable name="requestUser"><xsl:value-of select="//field[@title='First']"/>&#160;<xsl:value-of select="field[@title='Last']"/></xsl:variable>
  <xsl:template match="/entry">
    <requests>
        <request>
        <target type="sysaid">
            <url>https://libticketingdev.umd.edu/webformsubmit?pageEncoding=utf-8</url>
            <formId>1300b3c2:12460019b64:-8000</formId>
            <accountId>umlibraryitd</accountId>
        </target>      
        <title>Create a group</title>
        <category>Web</category>
        <subcategory>Libi (Intranet)</subcategory>
        <email><xsl:value-of select="//field[@title='Email']"/></email>
        <firstName><xsl:value-of select="//field[@title='First']"/></firstName>
        <lastName><xsl:value-of select="//field[@title='Last']"/></lastName>
        <description>
        Request User:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@title='First']"/><xsl:text> </xsl:text><xsl:value-of select="field[@title='Last']"/> (<xsl:value-of select="//field[@title='Email']"/>)
        
        Name of group:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field334']"/>
        
        Group description:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field335']"/>
        
        Minutes folder:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field336']"/>
        </description>
        <usmaiCampus><xsl:value-of select="//field[@title='USMAI Campus']"/></usmaiCampus>
      </request> 
      <request><!-- AlephRx request -->
        <target type="alephrx">
            <url>http://alephrx.local/cgi-bin/api/reports</url>
        </target>
        <name><xsl:value-of select="concat(//field[@title='First'],' ',//field[@title='Last'])"/></name>
        <functional_area>other</functional_area>
        <campus><xsl:value-of select="//field[@title='USMAI Campus']"/></campus>
        <phone>301-555-0123</phone>
        <email><xsl:value-of select="//field[@title='Email']"/></email>
        <status>new</status>
        <summary><xsl:value-of select="//field[@title='Description']"/></summary>
        <text>text</text>
        <submitter_name>A.N. Other</submitter_name>
      </request>
    </requests>
  </xsl:template>
</xsl:stylesheet>