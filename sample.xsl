<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:variable name="requestUser"><xsl:value-of select="//field[@title='First']"/>&#160;<xsl:value-of select="field[@title='Last']"/></xsl:variable>
  <xsl:template match="/entry">
    <requests>
      <request>
        <target type="SAMPLE_TYPE">
            <url>SAMPLE_URL</url>
            <formId>SAMPLE_FORM_ID</formId>
            <accountId>SAMPLE_ACCOUNT_ID</accountId>
        </target>
        <title>SAMPLE_TITLE</title>
        <category>SAMPLE_CATEGORY</category>
        <subcategory>SAMPLE_SUBCATEGORY</subcategory>
        <email>SAMPLE_EMAIL<xsl:value-of select="//field[@title='Email']"/></email>
        <firstName>SAMPLE_FIRSTNAME<xsl:value-of select="//field[@title='First']"/></firstName>
        <lastName>SAMPLE_LASTNAME<xsl:value-of select="//field[@title='Last']"/></lastName>
        <description>
        SAMPLE_DESCRIPTION
        Request User:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@title='First']"/><xsl:text> </xsl:text><xsl:value-of select="field[@title='Last']"/> (<xsl:value-of select="//field[@title='Email']"/>)

        Name of group:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field334']"/>

        Group description:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field335']"/>

        Minutes folder:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field336']"/>
        </description>
        <usmaiCampus>SAMPLE_CAMPUS<xsl:value-of select="//field[@title='USMAI Campus']"/></usmaiCampus>
      </request>
      <request>
        <target type="SAMPLE_TYPE_2">
            <url>SAMPLE_URL</url>
        </target>
        <email><xsl:value-of select="//field[@title='Email']"/></email>
        <firstName><xsl:value-of select="//field[@title='First']"/></firstName>
        <lastName><xsl:value-of select="//field[@title='Last']"/></lastName>
      </request>
    </requests>
  </xsl:template>
</xsl:stylesheet>