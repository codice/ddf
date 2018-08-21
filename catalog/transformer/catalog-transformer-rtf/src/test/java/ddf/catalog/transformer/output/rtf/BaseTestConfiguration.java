/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.output.rtf;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.transformer.output.rtf.model.ExportCategory;
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseTestConfiguration {

  static final String EMPTY_ATTRIBUTE = "Empty";
  static final String SIMPLE_ATTRIBUTE = "Simple";

  private static final String UNKNOWN_ATTRIBUTE = "Unknown";
  private static final String BASE64_IMAGE =
      "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAkGBxAQEBUQEBIVFhAVFRUWGBUVFRgWEBUVFRIWFxcXFxUYHSggGBolGxUWITEhJSkrLi4uFx8zODMsNygvLisBCgoKDg0OGhAQGi0mICUtLS0tMDAtLSstLy0tLy0vLS0vLS0tLS0tLS0tLS0rLS0tLS0tLS0tLS0tLS0wLS0tLf/AABEIAOEA4QMBEQACEQEDEQH/xAAcAAABBQEBAQAAAAAAAAAAAAAAAgMEBQYHAQj/xABBEAACAQIDBAcFBgUDAwUAAAABAgADEQQhMQUSQVEGEyJhcYGRBzJCUqEUI3KxwfAzgpKi0RWywiTh8RZiY3PS/8QAGwEAAgMBAQEAAAAAAAAAAAAAAAIBAwQFBgf/xAA8EQACAQIDBQYFBAIBAQkAAAAAAQIDEQQhMQUSQVFhEyJxgZGhBrHB0fAUMkLhM/FSFSMkNENigpPC0v/aAAwDAQACEQMRAD8A7jAAgAQAIAEACABAAgAQAo9pdLMHQuDU33Hw0+2fAkdkHxIm6js7EVc1Gy5vL+zHWx9Cnk3d8ln/AEZrHe0NzlQoqP8A3VCWP9K2t6zp0tiR/wDMl6fd/Y51Ta8v4R9fsvuVq9Ido4g3FbcS/wACqo8jbePrMO0sXs3ZicZLeqWuo5vwvwS8fK5VHFYqtmpWXRL/AGVuNrVyxDYxj3PWcH0vaW4DaKrUVUeEkrpaRi0+qd0/YpqSmpWdX1bIbYR21qofGpeaf+qQTsqFX/42vfJCbkn/ACXqRHVkNgdOKk29Z0YSVSKla3R6+gt3F6i6e08QnuV6q/hquPyMWVCm9Yr0Q8a01pJ+rLTZ/SraYYLTrs5+VwrX8SRf6zn4ylgqFN1K1ornmjTTxeIvZSv6Gv2b0rxgNsRSosPmpsyn+kgg+onkcRtnAJXo77fJpfO69rnQp4up/NLyNDhOkGHqZFtw8nyH9Wn1i0dqYeplvWfXL309zVHEQl0LRWBFxmPpOhe5eewAIAEACABAAgAQAIAEACABAAgAQAIAEAPCbZmAGW2/01o4e6UlNWpz0pD+b4vL1E37OwSxke0jNbumTTf9eZzcVtKFLuxV37evHyMLtLbmLxh3ajkg6U07NP8Ap4+LEzv9lhMBTdWVopayf57LU4tXE18Q7N+S0/PE92bsCtVzYbiC4ucjcG2Xd3zm7Q2/GjGKw0d+UknxUbNXTva7fRLxsNRwc565Iv8AC9HaNIbzsWPl9P8AxPN4jGbR2jLst7dXKN4+stfl4G+GDp083mWOFw9I3ATTnnlMGL2SsNaUu9fV65+eviaKag8kiScOum6LeAmek5Unem7eGRY4rkQMbgrZoDbj+9Z6TZu03UbhXkr8MrX8eBnqUrZxK50ndKCLVoKdQD4gGMm0Q0iDW2fTPw2PMZflG7SVrMR048iRSxJRQpBa3EntHxyznlcb8NU61SVSlPdvw3cl4WasWqVlYn0u2u8uY/I8jynkcXga2FqOnVWfs+qLFmrkjCY2rRN6bkd2qnxU5SqhiatD/HK3Th6DwqShozRbO6Tq3ZrDdPzDNPMaj6zu4bbMJd2srPnw/r5dTXTxSeUi/RwwBUgg6EG4PnOymmro1J30FSSQgAQAIAEACABAAgAQAIAEACAEPaW0qeHXeqHM6KPebwH6zPicVTw8d6b8FxZXUqRgrsxm1Ns1cQbE7tPgg0/mPxTy2M2hVxGWkeX35/I59StKeuhRjZGJxNQAgJTGjHMWPED4j6T3Wxa2z9n4J1qMnKUrXurO60VuC1tr4s50sPWrzs8kavZ2xqNAdhBvfMc2M4WJxNbFS3q8nLO65LwWi5c+p1KWGp0v2ondQvIekFXrJWU36st3FyGMXhha4AFvrOjs7Fz7Tcm276cbFdSmrXQxhMm1sON5v2lT7Sg7K7TVrfnIqp5SJ5WeXaNQkrF3b5EFLjrM5K6frPY7Op1KWHjCpr9DFUacrohVFm8qGlwzObKD48B4mU18TSoRcpvy4vwXElRb0I+Nwb0z2h4EaGJhcbRxKvTfiuKCUHHUg77IbqSD3S6rRp1Y7tSKa6ip20LrBYtKwscqg1HPvHMTwW1dkSwsnJZwej5dH1+fsXxakLqUiJwpQaBof2ftKrhzdD2eKn3T5cD3iX4XGVcO+48uXD+vzUenVlDQ2OytrU8QOzk41Q6jvHMd89RhMbTxK7uvFcf9dToU60Z6FhNhaEACABAAgAQAIAEACABACp25tpcON0WaqRkvAd7d3dx+s5+Ox8cNGyzk9F9X+ZlFasqeXExVatUrPvOSzt+7Acu6eUqVKlae9J3b/MjnNuTu9S32fskDtVMz8vAePMzo4fApd6pry/NS+FK2bLdVnSUS+wsLGsTYYxTaAHxtrOpgcOu9OcfC/wCe5XUfBD1SkGFjMNKpKlLeiO4pqxWvTs1jmAZ6OM3Upb0cm1kZWrMsd2eUlFp5mojY591dNcvUGbNn0FVrq7tbP0aK6rtEp2E9WjEMVFjCiKWIamcjlxHA/vnMmLwNLExtNZ8HxX9dBozcdA2jtAVEKhbXIzJ0tMGA2RLDVlVc72vlbmNOrvK1ijqrO4UkV7jMZH6wcU1ZoCXgtpPhxu1VbcIuuWY8L6iee2jsqljpdph5R3llLl520a9yyMnHUl7GxHW0hvMDUFweeuRInm9uYCOGxL7ONoO1uXVImOaJaOysGUkMDcEZEGcWMpQkpRdmiU2ndGw2Dt4VrU6lhV4H4X8OR7vTu9Ps/aSr9yeUvZ+HXp6dOhRrqeT1LydU0hAAgAQAIAEACABACn6RbcXCplnVIJA5D5m7uQ4+tqa85xUY01ecmoxXNv6LVvRcdUZsRiFSj1MQrNVbeuWZze/E3ni6sKrrSjUvv3afO6yfp6HPTc89bmj2bs8Uxc5udTy7hOvhsKqSu9TXTpqJYqs2pFo4FjqN8kSAKkd00djUhNK2YXTRHoBQcx58p08TCpOPdflzKo2TzJFcADPXh4znYeEnLu6cfAslaxW1Bc3ncglGKiuBmZIwtYW3TrwnKx2Ee86kFk9SyEuDEbTXsDx/Qxdlf5n4fVEVv2lSwnokZGMOIyFI1QRhSLVEkCHVEkgiVYAW2zsf9oJo1UUjdJvbLKw04HPUTx+1NnfoEsTh5tO9vXrxWWjvcujLeyZVbT2NVot1tAkqM8j21/8A0P3abMJtnD4uHY4pJN5Z/tf2f4mK4NaFxgapqUUdrbzKCbaX4zx+0aEaGJqU46J5eAyzQ5ppr9Zhu07oDY9HNtdcOqqH70DI/OB/y5+vh6jZu0O3XZz/AHL3/vn6+HQoVt/uvUvZ1jSEACABAAgAQAr9u7WTCUWrPnwVeLudFH7yAJmjDYeVeooR8+iKMRXjRg5y/wBs5cuMq4uqWq53za2jG/ZXwGQA7obcp0tmUniKbvVk1GF89xW726uet3reXr5+FSeIn3/P6Gy2VgRTFz7517hyE8lhcPud6Wcnr+fM7FOnuos0E3JFw6oliRI29S+Q0/OdbD4Xc70tRJSvkgp07kTTUk1FtEJExqQItacuNScXe5c4oimmTcLe3fNrnBbsppX6FVnmkQ3E3IpYw4joVjdSoxFiTblIjRpxlvKKuK27WIzzQitjLx0IyNUjCkSrJIIdWSQRKskBGCxnU1N/d3siLXtr3zDtDB/q6Dpb1tHe19PQaMrO5ov9SpFUfesKh3VyN969rG2mc8FU2ZiFUqU1G+4rvNaa3z5rzL95WuQqQSniWppftp1jLnuqwa1+4tvf2iNiHVrYCFWp/GW5F8Wradd22Xi+oujJbCcUBKuVIZTZgbgjUEQjJwkpRdmgTtmje7C2oMRTucqi2DDv4Edx/wAz2OBxixNO/wDJar84M6dGr2kepZTaXBAAgAQA8JtADkPS7bhxmIJU/cpdaY555v4tb0AnrMBhVQp56vX7eR5jG4ntqmWi0+/mXfRjZm4odhnw8eflp6z57tDG/wDUcW6/8I5QXTjK3V+1r6G7CUNyN3qaRJWjePJLESOWuJfTe7JPkyeAyEI1nbjUjNXiyu1h6kcxEqLuseJMM5jLRBitkFZjE3T3GdbC1VOCXFGaorMhuZsRUxhzLEVsYcx0IxljHQrI1QxxSLVkkEOrJIIdWBBCrQJIVUyGgJOydsLQd3qBnZgBvXu2Xjrw48Jw9rbJni6cKdFqKjfK2Wfhpx4cR4uxqMDi1r0xUS9jfI6gg2IPpPB4zCTwtaVGeq9HcdO44xF7XzPDibTLutq9sgJGzMc2HqioumjD5lOo/fES/CYmWHqqa049V+aFlOo4SudFo1VdQ6m6sAQeYM9rGSlFSjozqppq6FxiQgAQAyPtG2z1OHFBD95WuDzFMe96+75nlOpsvD9pU33pH58Puc7aVfcp7i1l8uP2MDsDBddWVfhGZ8BNHxBi5UMG4wdpTe6nyX8n6XV+bRxsNS7SokdFpKAABoJ4SCSVkd1D6S1DIeWWIYdWWIYXYHIy2FRwd0GojCjj32m3FVHlHpcWHMkXmK5YJJkNkCHAIsdJCm4u6ZDzKXEoVYg/sT0VCoqsFJGOas7EVzNCRUxlzHQjGXMsQrI1UxhSJVMkgiVTAgh1TJAhVjACFWMgkg1c/GK8s2Sanotg8TS3hUXdpEXAJG9v5ZgDQWve/ITwvxDisFiN2VGV5rK6vbdz48c9LX4lkUyL00q1abUnQ2VSbEe8Ht+W7+t43w5ToVYVqc1du11w3fvf6WGPdidIzVulUDrACV3custqAD8Vs7ce6U7U2EqDU6L7rdnfPd5O+u7f06hY6J0I25Ta2H3wd9WqUs9VBXfA82Bt3tyluyHVjGdGorbjS8L3y9vc24Wp/BmxnWNgQACYAcS6TbV+14qpWvdL7qf/AFrkvrm3809bhKPY0lHjx8fzI8xiqva1XLhw8PzM0fQ7DBaPWW7Tk+gNvzBnjPiKq543dbyikl55v1yv4I24GFob3M0iTjI2jqmWIYeUyxMYcUx0yRYMa5IU1A053ltSq5tN8rEJWF3ldyRtsQoNsyeQBMujQnKO9oursK5rQg18YzHcUWz4HP1Gk6FHB04R7Sbv5Zf2UyqNuyPNo4awLgnK2WvdrFwGLu1Sa556exFWH8ipZp20jK2Ms0dIVsZdoyQjI1RoxBFqtJIIdVoEEOq0AIVVoEkKs0gCFUMhjI3mwNo/aKIYn7xey/jwPmM/WfM9sYD9HiXGK7rzj4cvJ+1i1O5E6XYPrMOWGtM7/iADvD0N/KW7AxXY4tRek+758Pt5kmM2Xs6tXYmlcFATvZizAXAB4Emeu2hjcPhoJVtJZW1y4u3JcfQY82TtSpha9KuhO9Ra4Un4bnfTuDAsD+IzW6cWnZLva9eT68Boy3Wmj6VweJStTSrTN0dVdTzVgCD6Gcppp2Z1E7q49IJKDp1tL7PgahBs7/dLzu+Rt3hd4+U2YGl2ldJ6LP0/sy42p2dFtavL1OPYWiXZUX3mIA856SrWjRpyqT0Suzzqi5NRR03CUVpoqL7qgAeU+YV6869WVWerd/zwO3CKikkS1MVMsHVMsTJHVMdMYcUx0yRYMm4CgZNyQYm2WsaMkmm9CGMYiker3U7vPnNVDER7ftKv+uQkovdsiv6tqY6w5FTax7xr9Z1XXp4iXYxzTV7r5FFnFbwhNpuAQbN4/wDaTU2XSk043XgKq0lqVzNOokUNjTNGSEbGXaMKRajSQItVpJBDqtAgiVWgSV9esBJsRchVKhMhkoYaIx0TujuOFHEKWa1Nrq3KxGRPgbZ+M422sG8VhJRiryWa+vqrjxNN0g2lQ+zuodXZ1KqqsGYlhkbA6DXynkNl4HE/qoTcHFRd22mlZePPQcd2DhjSw1NSLNa5HG7G+ffnMm1q6rYypNO6vZeCy9P9gyjxexWpYoYgLv0TU3mUAl1Lcd3iAxvl6Ts4fa0a2BeGct2ajZNuyduvBtZZ+oXyOu9DMVvUDT40zb+Vsx9d4eUNj1+0obrecX7PNfbyOhhZ3hbkaCdY0nNfazjr1KOHB91WqMO9jur/ALX9Z2tlQspT8vz2ORtOecYef57me6IYfer750RSfM5D9Zl+JMS6eGVJfzfss/nYx4WF6l+RukM8QmdMeUyxMYdUx0yRxTHTJHAYyZIsGNckUDC4AzgC50EaKcmorVg3Yhf6qt21sBlzJ/SdX/pNTdjzbz6L6/niUdursq8Timc3Y+A4Dwnbw+GhQjuwXnxZmnNyeZHZ5pSEbG2aMkK2NM0ZIW4xUeSQRajySCLVeAEOtUkkFXicQTkNPqYyjYW5DYyGShpjK2WIaYxGOhpojGRougyr1lU/EFW3OxJv+SzynxRKXY01wu7+mX1HNc08WQNtFAu+huJ3cTucHUjzXtD6BvWdbYtXdruH/Je6z+5owsrTtzN1PVnSOK9P8V1m0a2eSbqDwVBf+4tPR4CO7Qj1zOBjZb1aXTImdCU7NRuZUegJ/wCU838Uz79KPRv1t9icItWapTPLJm0dUyxMkdVo6Yw6rRkyRYMa4CwY1yRQMLgQcbj03WQXvpple87eB2fXVSFV2trqUVKsbNFOXnpLGS4gvJsRcQzRrC3G2eNYi4y7ybEXI9R5JBGqPACJVqSQKnFV94935x0rCN3IrGQyUNMYjHQ2xlbHQ0xiMZDTGVsdGj6Ckb9XLPdXPkLm4/L0nlPii/Z03fK7+X+/UY1zTxhA20gB7Zdbcr035Ot/Amx+hM0YOe5iIS6oam7TT6nT57k7B8/barb+Krv81aqfI1GtPUUVanFdF8jzlV3nJ9X8zQ9DalkZeZ3vSwP6Ty/xTS/x1eV4+ua+TLsNkmjUK08kmax1WjpkjqtHTJHFaMmSOK0a5IsNJuAxjcX1YB3b3y7h4zobPwaxU3Fytb1fgJUqbivYoHqXJJ1Oc9tGCilFaIwN3El41iLiC8mxFxDVJNiLjT1JNiCO9SSQMPUgBGqVJIFbjK9+yPONFcRG+BCYwbBDbGIx0NMYjHQ2xiMdDTGIxkNMZWx0aXoPg2LvXvZQNy3zE2Jv4Zes8p8TYmKhGhbN97wWa98xma1p40UbaQA20gg6J/rA/f8A5nuP1KOr2yODVKl2ZubE+pvPZxyVjgyzbZqOiBz8FP8AuE838Tzth4rnJfJl9A1StPFJmkeVoyYw4rR0yRxWjJhccDRrkiw0m5JE2xVApEcSQB63/SdjYcHPFprgm36W+pTXdoGf6ye2sYRJqybECGqySBpqkAGnqSSBh6kAI71JIEPE17aayUiGyAxktkJDbGI2MkNsYjY6G2MRsdDTGIxkNsYjHQ0xiMdGz6DA9Q+WRqG3f2FBniPiZr9RDnu/VgzQtPNCjTSAG2ikEn7e06P6tjb7OfuLMRyJHoZ9Zi8jNJWZddHcTuOnIndP82X52nM2zQ7bB1FxS3l5Z/K6HpuzNrTqXv3G0+cyi42vxV1+eN14o0pjytBMYdVo6ZI4rRrkjgaTcLiw0ZO+hNzLbRx5qOTfsgkLytz859D2bgo4WglbvOzl48vLQwVJ70iKas6JUJNWSAg1YEDbVZIDL1YEDD1ZIEerWtACE73kkDbGK2MkNsYjYyQ2xiNjpDbGI2MhpjEbHQ2xiMZDZMRjo3fQ0/8ASjP438tMv1854P4i/wDG/wDtX1FZctOCQNtFIENIAd+yPym39K+RO6zE7Yp7mKrp8taqvpVYT6zSd4RfRFdRWm/F/MVs+tusDyYH0I/xIrw36co8016oRG9oP23HPdb1G7/wny6pnQpS5b0X5Pe/+xpWpLVpQmMLNQAEk2AzJ4ASyClOSjFXbyRNyu/9R0r+69udh+V56RfDOK3b70b8s/nYq7eJErbfqb5KEbl8gV4d/H6zr0Ph/D9glVT37ZtN69OHsVutK+Ra0cd9poOFuHC2I7yDax5GxE4tTB/9NxtKVRpwbyfTr1V7lqn2kHbUy/Wz3hiDrYAJNWSAg1ZJA01WADTVYAMvVgBGd7wAbJitkpCGMVsZIbYxGxkhDGK2OkNsYjYyQ2xiNjoaYxGMhMgY1/QV+xVW+jKbcrgi/nu/SeO+KI2qUpW1Tz8Gvlf3FZpWnlRRtpBA20gg33+id379Z7T9IdPsTkntAw3VbTxA4MyuO/fRWJ/qLek9XhZXpRMGJjaqyipvaaTObPYW0A9Jb+8lkPgbBT+Q9Z4DbWClRxM1H9s7zXis5L0u/Qti8i8Vp55Mchbfq2w7eKj+8Tu/Dy3sfDopP2YtR90yYqz6OZRYqySCx2HtMUat2PYIs3G3EG3j+ZnK2zgJYzDbsP3J3X1XmvexZTnuyzHcbVRhVdKW8m/cVRdd0tYkEcRc/WVYPtacqNOrV3ZbtnTdne10mnwdtbXvbxCVndpeZVGrO4UiTWkgINWADbVYANtUgA2WkXCwgtFbGSEForYyQgmK2MkIJitjJDbGI2MkNsYrYyQ2xiNjpCDFJCBJregz9mqvIob8cww/4/WeP+KY9+lLpL2t9xZGmJnlBRsmKQPbOo9ZWpp8zqPIsL/SX4WG/WhHm0NBXkkdUnvDsnJPbNgd3EUcQBlUpmmeV6bbw8yKh/pnTwM+64nOxse8pHPg06CZisScJi2pklTkRYj4WHf/AJ4SjEYaniElNZp3T4p9PqtGsmRobvZWL62ktTiRn+IZH6ifMto4X9Jip0Vonl4PNfYtTuj3a9BqtB0X3rAjvKkG3na3nLtkYqOGxlOpPS9n4NNX8r3IkrqxhxVn1QzDiliCQCQNSAbDxPCQ6kU1FtXemYFlgqmEVQaxd3PwpcBB3k2ufCcrFy2lUqOOGUYRXGWe94JXsvHMZbiWZYtgEq0mbBVWPzUi1r9xBtY+ORnKjtOrQxEY7TpRVv2zSvbwefnazXFDuCa7jKDEo9Nt2opVuRFvMcxPU0MRSrw36UlJdChprUZLy25B4WhcmwktIuFjwtIuTYSWkXJsJLRbjWEForZNhBaK2NYQTFbGsNkxWxkhDGI2MkIJijHkCQgBpOhmNVXaiRm/aDfhU3B8rn1nl/iXCSnTjiE8o5NeLWa89RZGtYzxYg2ZAF50Ow2/ig3BFZu69t0f7r+U6uxqW/id7/im/p9TRhY3qX5HQJ646Zkfajszr9nOwHboEVh4KCH/ALGY+QmnCz3ai65GfEw3qb6ZnDQ066Zy7Cg0ZMixa7G2y2HNrXpk3I4jgSO+3DunI2rsenjlvXtNKyfPo+l/QE7G7pOGAYG4IuCNCDoZ84lCUJOMlZrJosMtt7Yrh+soqWVzcqouVY9w4H6T3OxNu05UlRxElFxWTbsmvPivfXmVShxRoMHQNLCbhHaFNrgfMVJP1nnMTiY4naXap5OcbeCaS+5YlaNjAhp9PuZrErZrP1qdW4RybBibAeJ5d0yY7sv08+2jvRtdpK78uvyJinfIt8ZtOsK/U4l6bU1ZS33YZLWDWFhvd3/acbC4DDPC9vhITjJp27zT4q7z3eq6ZrMdyd7Md2vi9nMo6tCWzt1a9XbL4rgA+hlWzcPtiE32s+7/AOp79/CzuvVBLc4IoHFPq1Ic9bchlK5AcCG4+HfPRRnV7WUZRW5lZ3zfNNfIrsrCayqACrFhYX7JXdY/D36a8ZNOpJtqatm7Z3uufC3VcOZNhktLLhYSWkXJsJLRbjWElotybCCZDY1hBMRslIQTFbGSEExRjyBIQAIAKpVWQhkJDDQjIgxJ04VIuE1dPVMg6PhcStVA6G6kfXiD33ny3FYeeHqypzVmvlwZWzMbT2nVrYgUaLFVDgBlvckaseajPLTKeswGzaGFwbxGIjduLbT4Lglyby63yGSyzOxdB8Ju0mqnV2sPwpcfmW9Jk2JR3aLqP+T9l/dzdhIWi5czSztGsTUQMCrC6kEEHQgixEAPm7pFstsHiquGbRGO6T8VM5of6SL9952qdTfipHIqQ3ZOJADS25XYUGk3IsW+yNvVMON22/T+UmxH4Tw8Jx9pbFo417992fNZ38Vx8bgnY0NHpVhyLsHU8t2/oRPN1PhnGRlaLi1zvb2f9k7yLyjVWooZSCrDIjQgzhThOjUcZK0k9OoxkD0UxA0ameXaYE/2z3S+KsI9YzXkv/19Crs2UbZEg6g2PlPRxkmk0LY8vGuRYN6Rcmx5vQuFhyg+q3yIJtv7q3AJBN9bHhxlNXK0uKfK7s3n4X4vhqyUhtwRa4tcXHeLkX9QfSOpqV7PTJhYbLQuTYSWkXJsJLRbjWEForZNhJMVsawgmQSECQgAQAIAEAJGHx1WmrIjlVbUD9OXlMtbBYetUjUqQTlHR/mvmRYuug2DariDui5ChR+J2AH0DTkfEUnKjChHWcvZf20DTdkj6BweHFKmtNdFUDxtxj0qapwUI6JWOrGKikkPSwYIAc19smwOspJjqY7VLsVLamkT2W/lY+jk8JswlSz3WZcTTut45GGm+5hsKDRrkWFBpNxbHoaNcixO2ftatQ/hvYHVSLrfwOnlMOM2dhsXZ1Y3a46P1X1DQ2PR3bgxA3GsKqi5tow5gfmO+eL2xsh4OW/DOD9nyf0f42TMjtnDtSr1FYWuzMORVmJBHOe12biYV8LCUHeySfRpZpiNESkrMwVQSxNgBqTNs6kYRcpOyWrCxp8P0XIok1c6rNTAC5hVNRQ2Y1O6TnwtPLVfiNSxCVL9iUs3xe67eV/N3G3T3avRRy5ahu7rMeyTYIthbPjnvfSGB+JKapqOIvdLXW7z+lve4OPIX/6LyX7/AD+LsZfy55ecrfxVnL/svDP55fL+w3DM7VwvUVnpcFbLmVOa/QiejwWK/U4eFXms/Hj7kWIZaabk2PC0i5NhBaLcawkmLcmx5AkIAEACABAAgAQAIAdh9kuwOrp/aXHaOl+ZGXop9WPKeblP9Ti5Vf4w7sfH+T+how0LvffA6RNRuCABABvEUFqI1N1DI6lWU6FWFiD3EGSnZ3RDV8mfOnS/o++z8U1A3NP3qbn46Z08xoe8X0InVpVN+Nzm1IbkrFNeWFZ6Gk3IsKDSbkWPd6TcLDlGuyMHUkMpuCNQYtSEakHCaunqiLGhfb2HxKqmLpkMCLPT0F9Tmbgd2c85HZOKwU3PBTTTWcZceWmT6PL3AsNnLs/7SqUf4idpXDEq5KkFbnIkA3y/yJhxdTajwkqlb9ssmrWcVdWeXPTO/Xgwsai88ySF4Enu9ACDtDZdCv8AxKYLWtvDJx4MM5swu0MRhv8AFNpctV6EGWqdC6u8d2qm5fsk337d4Atfwnpo/E9LdW9B342tbyzuBUba2HVwti5VkY2DLe17aEHQ6+k6WA2rRxt1C6a4P5olFVOkMEACABAAgAQAIAEACAFv0U2M+NxVOiouLgseAUa3/fOY8dWdOlaH7pZL7+Sz9FxJjFydkfRmCwy0qa0091Rb/JPeTnObRpRpQUI6I6cYqKSQ9LBggAQAIAZzpz0YXaOGKCwrpdqTng1s1J+VrWPkeEto1ezlfgVVae/G3E+fcTQem7U6ilaiEqynVWBsQZ1E01dHOas7MbkgEAPbwIC8LhYN6TcLC6NdkYOvvKQw8VNx+USpCNWDpy0aafgwsdYpvcA6XANuWU+VyVpNIUXeQAb0AC8APLwArukDL9lq767w3Dla/aOSnyJBvwtN2y994ymoOzv7cfVXXUEYbBdH8TVXfVLLw3juk+AOdp7XEbYwdCe5OefRXt6DXItTZtdWKmjUuDbJGI8iBYjvE1Qx2GlFSVSNvFfiC41Xw1SnbfRlvpvKVv4XEspV6VX/AByT8Gn8gGpcSEACABAAgB6ikkBQSxIAAzJJNgAOchtJXZB3/oD0XXAYcbwH2hwC54jju3nCTlUm6s9XouUeC8Xq+vRI30KW4rvVmplhoCABAAgAQAIAYL2ldCPti/asMo+1IO0unXINB+McDx0PC2mhW3O69DPWo72a1OKMCCQQQQSCCLEEZEEcDOiYTyABAAgAQAfwDIKtMv7gdd7w3heUYpTdGap/us7eNiDqd58tQh7eBIXkgF5AHl4EBeACS0gDwmADGLpJUQpUAKEZ308b8Lc5ZQrVKNRTpO0lp9ut+XEDmDWubaXNr624T6qr2V9Sw8kgEACAATADrXsv6DmmVx+LW1TWjSYZoCP4jj5raDhqc7buHE1k+4jXQo270jp8xmsIAEACABAAgAQAIAYH2g9ABjL4nCgLitWXRK1uZ0V+R0Oh5jTQr7vdloZ61HezWpxevRZGKOpV1JDKwsykagg6GdBO+aMLVhEkAgAQAIAbvoztdKlJKRa1VRu7p1YKMiDxyH0M8FtvZ1WjXnWjHuN3vyb1T5Z+QjReXnCIC8AC8AC8APLwA8JkAJJgBl+l21bf9OmpALm/A6L55E93jPV/Duzt7/vc+qivr9ESkZSewHCABAAAvkMycgBqSdABzgB1r2e+zvqyuLxy9sWanQOe4dQ9QfNyXhxzyXDXxF+7E2UaFu9I6fMZqCABAAgAQAIAEACABAAgBmOmPQrD7RXeP3eJAstZRn3K4+NfqOBGd7qVaVPwKqlJT8TiXSHo7isBU6vEJYE9moM6T/hbn3Gx7p0IVIzV0YJwlB2ZVSwUIAEAFU3KkMpIYG4I1BEWcIzi4yV09UBrtjdJ1ayV+y+m/ojePyn6eE8ZtL4fnTbqYbOP/HivDn8/ERo0auCLg3H0nmmmnZ6kBeQAXgB5eQQeXgB4TADm+PxPW1XqfMxI8NFHoBPqeDw6w9CFJcF78fV3LER5pJCAE7Y2x8RjKnVYamXfjbJVHN20UePleLKcYq7JjFydkdo6FdAKGAtWq2q4r57fd0+Ypg8eG8c/C9pz6tdzyWhupUFDN6mzmcvCABAAgAQAIAEACABAAgAQAIAMY3B0q6GlWRXptqrAFT5GSm07ohpNWZzLpN7KNamz3tx6moTbwSoc/Jr+ImyniuEzLPDf8Tmu09m18K/V4ik9N+AcWvbip0Yd4JmuMlJXTMsouLsyJGICABACRgcbUotvU2seXwnxHGZsVhKOKhuVY3+a8HwIsbTYm2lxAII3ao1XgRzXu/KeD2rsmeCkpJ3g9Hy6P78RGrFneccg8vABLOACSbAZk8ABJjFyaitWBk9tdIesU06IIU5FjkSOQHAGe12VsDsJqtXaclouCfNvi16XGUTPz044/gsHVruKdGm1SofhRSzeJtoO85SG0ldgk27I6L0a9lFR7VMe+4uvU0yDUPc9TRfBb+ImSpiksoGqGGbzkdS2XsyhhaYpYemtOmOCjU8ydWPec5jlJyd2a4xUVZEuKSEACABAAgAQAIAEACABAAgAQAIAEACADGMwdKshp1qa1EOquoZT5GSm07ohpPJmI2z7KsFVu2HZ6DcgespX/Axv5BgJojiprXMolhovTIxm0/ZZtGlc0urrLw3W3H81ewH9RmiOKg9ciiWGmtMzNY3o9jaN+twtZQOPVsU/rUFfrLlUg9GipwktUyrLAGxOfLjHEuP4HFNSqLUXVT5EaEeYMz4rDQxNGVKej/E/JhqbrZm0qeIXeQ5j3lPvL/kd8+c4/Z1bBT3amj0fB/30K2rEyYEr6AU20hi8UDRwmFrsrZNU6pgjDkrEWseZInsdkbG/TzVfENXWi5Pm+q5FsaUnwFbN9l+0qtjUWnRX/wCRwz27lp3HkSJ6SWJgtMy+OHm9cjY7H9k2Ep2bE1Hrt8o+6peine/umeWLk/25F0cNFa5m52ds6hh06uhSSmnJFCg95tqe8zNKTk7tmhRUVZEqQSEACABAAgAQAIAEACABAAgAQAIAEACABAAgAQAIAEACAGb6Xe75S2mJM41t/wB8zfTMVQn9A/4v8pmHan+HzQsdDsexv39Jgw+hspFvNJcEACABAAgAQAIAEACABAAgAQAIAf/Z";

  public List<RtfCategory> getCategories() {
    return Arrays.asList(
            "Associations",
            "Contact",
            "Core",
            "DateTime",
            "Location",
            "Media",
            "Security",
            "Topic",
            "Validation",
            "Version")
        .stream()
        .map(this::categoryFor)
        .collect(Collectors.toList());
  }

  Metacard createMockMetacard(String title) {
    Metacard metacard = mock(Metacard.class);
    when(metacard.getTitle()).thenReturn(title);

    Attribute mockMediaAttribute = createMediaAttribute();
    when(metacard.getAttribute(Core.THUMBNAIL)).thenReturn(mockMediaAttribute);

    Attribute mockEmptyAttribute = mock(Attribute.class);
    when(metacard.getAttribute(EMPTY_ATTRIBUTE)).thenReturn(mockEmptyAttribute);

    Attribute mockSimpleAttribute = createSimpleAttribute();
    when(metacard.getAttribute(SIMPLE_ATTRIBUTE)).thenReturn(mockSimpleAttribute);

    return metacard;
  }

  Attribute createMediaAttribute() {
    Attribute mockAttribute = mock(Attribute.class);
    byte[] image = Base64.getDecoder().decode(BASE64_IMAGE);
    when(mockAttribute.getValue()).thenReturn(image);

    return mockAttribute;
  }

  Attribute createSimpleAttribute() {
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn("Simple value");

    return mockAttribute;
  }

  RtfCategory categoryFor(String name) {
    RtfCategory category = new ExportCategory();
    category.setAttributes(
        Arrays.asList(EMPTY_ATTRIBUTE, SIMPLE_ATTRIBUTE, Core.THUMBNAIL, UNKNOWN_ATTRIBUTE));
    category.setTitle(name);

    return category;
  }
}
