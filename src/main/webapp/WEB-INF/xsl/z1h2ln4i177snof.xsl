<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:variable name="requestUser"><xsl:value-of select="//field[@title='First']"/>&#160;<xsl:value-of select="field[@title='Last']"/></xsl:variable>
  <xsl:template match="/entry">
    <Requests>
      <Request>
        <Title>Create a group</Title>
        <Category>Web</Category>
        <Subcategory>Libi (Intranet)</Subcategory>
        <Email><xsl:value-of select="//field[@title='Email']"/></Email>
        <FirstName><xsl:value-of select="//field[@title='First']"/></FirstName>
        <LastName><xsl:value-of select="field[@title='Last']"/></LastName>
        <Description>
        Request User:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@title='First']"/><xsl:text> </xsl:text><xsl:value-of select="field[@title='Last']"/> (<xsl:value-of select="//field[@title='Email']"/>)
        
        Name of group:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field334']"/>
        
        Group description:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field335']"/>
        
        Minutes folder:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field336']"/>
        </Description>
      </Request>
    </Requests>
  </xsl:template>
</xsl:stylesheet>