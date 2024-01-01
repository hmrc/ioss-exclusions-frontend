package models.requests

import java.time.LocalDate

case class EtmpExclusionDetails(
                                 revertExclusion: Boolean,
                                 noLongerSupplyGoods: Boolean,
                                 partyType: String = "NETP",
                                 exclusionRequestDate: Option[LocalDate],
                                 identificationValidityDate: Option[LocalDate],
                                 intExclusionRequestDate: Option[LocalDate],
                                 newMemberState: Option[EtmpNewMemberState]
                               )

case class EtmpNewMemberState(
                               newMemberState: Boolean,
                               ceaseSpecialSchemeDate: Option[LocalDate],
                               ceaseFixedEstDate: Option[LocalDate],
                               movePOBDate: LocalDate,
                               issuedBy: String,
                               vatNumber: String
                             )