<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:variable name="requestUser"><xsl:value-of select="//field[@title='First']"/>&#160;<xsl:value-of select="field[@title='Last']"/></xsl:variable>
  <xsl:template match="/entry">
    <requests>
      <request>
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
    </requests>
  </xsl:template>
</xsl:stylesheet>