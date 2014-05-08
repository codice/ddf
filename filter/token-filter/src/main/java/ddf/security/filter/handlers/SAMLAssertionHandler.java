package ddf.security.filter.handlers;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

import ddf.security.filter.DeflateEncoderDecoder;
import ddf.security.filter.FilterUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;

//import org.apache.cxf.ws.security.sts.provider.model.secext.;

/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
public class SAMLAssertionHandler implements AuthenticationHandler
{
    public static final String SAML_COOKIE_NAME = "ddfSession";
    @Override
    public Object getNormalizedToken(ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve)
    {
        String samlString = null;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Map<String, Cookie> cookies = FilterUtils.getCookieMap(httpRequest);

        Cookie samlCookie = cookies.get(SAML_COOKIE_NAME);
        if (samlCookie != null) {
            String cookieValue = samlCookie.getValue();
            try
            {
                byte[] cv = Base64Utility.decode(cookieValue);
                DeflateEncoderDecoder decoder = new DeflateEncoderDecoder();
                InputStream is =  decoder.inflateToken(cv);
                IOUtils.toString(is, "UTF-8");
                StringWriter sw = new StringWriter();
                //OpenSAMLUtil.
            } catch (DataFormatException e)
            {
                e.printStackTrace();
            } catch (Base64Exception e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return samlString;
    }
}
