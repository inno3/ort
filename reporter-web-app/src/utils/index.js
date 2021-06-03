/*
 * Copyright (C) 2017-2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

/* eslint import/prefer-default-export: 0 */

/* Utility function to generate random numbers and letters string
 * Based on vjt@openssl.it public domain code, see
 * https://gist.github.com/vjt/2239787
 */
const randomStringGenerator = (length = (Math.floor(Math.random() * 501) + 20)) => {
    const rand = (str) => str[Math.floor(Math.random() * str.length)];
    const get = (source, len, a) => {
        for (let i = 0; i < len; i++) {
            a.push(rand(source));
        }

        return a;
    };
    const alpha = (len, a) => get('A1BCD2EFG3HIJ4KLM5NOP6QRS7TUV8WXY9Z', len, a);
    const symbol = (len, a) => get('-:;_$!', len, a);
    const l = Math.floor(length / 2);
    const r = Math.ceil(length / 2);

    return alpha(l, symbol(1, alpha(r, []))).join('');
};

// Utility function to generate color for given license string 
const licenseToHslColor = (license) => {
    let hash = 0;
    let min;
    let max;
    // Basic license classification based on https://scancode-licensedb.aboutcode.org
    const copyleft = new RegExp(
        'AGPL|BSD-Protection|CAL|CC-BY-SA-4.0|CECILL-1.0|CPAL-1.0|eclipse-sua|ecosrh|EUPL|ghostscript|gust|Glide|' +
        'GPL|hacos|ldpc|ldpgpl|lppl|NPOSL|Motosoto|ODbL-1.0|RHeCos-1.1|sleepycat|snia|SugarCRM|tanuki|tgppl|TMate|' +
        'TOSL|vhfpl-1.0|Vim|VOSTROM|YPL-1.1',
        'g'
    );
    const copyleftLimited = new RegExp(
        'acdl-1.0|APSL|aptana|Artistic|autoconf|avisynth|bison|BitTorrent|c-fsl-1.1|CATOSL|CC-BY-SA|CDDL|' +
        'CDLA-Sharing-1.0|CECILL|CERN-OHL-W-2.0|CPL-1.0|CUA-OPL-1.0|divx-open|dpl-1.1|eCos|EPL|EUPL|Eurosym|' +
        'FreeImage|GFDL|GL2PS|gnuplot|GPL-2.0-with-|GPL-3.0-with-|GPL-3.0-linking|gSOAP-1.3|IPL-1.0|Imlib2|' +
        'IPA|LGPL|LPL|MPL|MS-RL|NASA|NBPL|NGPL|Nokia|Noweb|NPL|OCCT|OCLC|OCLC|OPL|OGTSL|OLDAP|OSET-PL|Qhull|' +
        'QPL-1.0|RSCPL|RPL|RPSL|Ruby|Sendmail|SMPPL|SPL-1.0|TAPR-OHL-1.0|TORQUE-1.1|tpl-1.0|truecrypt-3.1|' +
        'UCL-1.0|vpl-1|wxWindows|YPL-1.0|Zimbra',
        'g'
    );

    if (license.match(copyleftLimited)) {
        min = 22;
        max = 48;
    } else if (license.match(copyleft)) {
        min = 325;
        max = 360;
    } else {
        min = 65
        max = 290;
    }

    const hue = Math.floor(Math.random() * (max - min) + min);

    for (let i = 0; i < license.length; i++) {
        hash = license.charCodeAt(i) + ((hash << 5) - hash);
    }

    if (min >= 65 && max <= 264) {
        return ((hash) => {
            return 'hsl(' + hue + ',' +
            (25 + 70 * Math.random()) + '%,' + 
            (55 + 10 * Math.random()) + '%)'
        })();
    } else {
        return 'hsl(' + hue + ',100%,50%)'
    }
}

export { randomStringGenerator, licenseToHslColor };
