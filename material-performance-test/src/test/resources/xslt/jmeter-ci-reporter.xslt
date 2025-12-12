<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/">
        <html>
            <head>

                <style>
                    h2 {
                    font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
                    }

                    table {
                    font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
                    font-size: 0.8em;
                    width: 100%;
                    table-layout: fixed;
                    border-collapse: collapse;
                    word-wrap: break-word;
                    }

                    thead {
                    background-color: lightgray;
                    }

                    td {
                    border: 1px solid #999;
                    text-align: left;
                    padding: 0.5rem;
                    }
                </style>

            </head>
            <body>

                <h2>Performance test - failed requests</h2>

                <table>

                    <thead>
                        <td>Thread group</td>
                        <td>HTTP Request</td>
                        <td>URL</td>
                        <td>Assertion</td>
                        <td>Response code</td>
                        <td>Response data</td>
                        <td>Failure message</td>
                    </thead>

                    <xsl:for-each select="/testResults//httpSample[@s='false']">
                        <tr>
                            <td>
                                <xsl:value-of select="@tn"/>
                            </td>
                            <td>
                                <xsl:value-of select="@lb"/>
                            </td>
                            <td>
                                <xsl:value-of select="java.net.URL"/>
                            </td>
                            <td>
                                <xsl:value-of select="assertionResult/name"/>
                            </td>
                            <td>
                                <xsl:value-of select="@rc"/>
                            </td>
                            <td>
                                <xsl:value-of select="responseData"/>
                            </td>
                            <td>
                                <xsl:value-of select="assertionResult/failureMessage"/>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>

            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
