(ns specialtyrepair.facts
  "Per-jurisdiction consumer-product-safety AND precious-metal-
  hallmark-integrity regulatory catalog -- the G2-style spec-basis
  table the Repair Shop Governor checks every `:jurisdiction/assess`
  proposal against ('did the advisor cite an OFFICIAL public source
  for this jurisdiction's requirements, or did it invent one?'),
  closely modeled on `cloud-itonami-isic-9524`'s `furniture.facts`.

  This blueprint's own named activities (watches, jewelry, bicycles
  and similar personal/household goods) routinely include jewelry
  repair/resizing work, a real, distinct regulatory concern beyond
  `repairshop`/9521's, `commrepair`/9512's, `applianceshop`/9522's and
  `furniture`/9524's own catalogs: several major jurisdictions require
  precious-metal (gold/silver/platinum) articles above a weight
  threshold to carry an authentic hallmark/quality mark, and a repair
  (resizing, soldering, adding replacement material) that compromises
  or falsifies that mark can itself be an offense (the UK's
  Hallmarking Act 1973 -- administered by four statutory UK Assay
  Offices tracing back centuries -- is one of the oldest, most well-
  documented consumer-protection-through-metal-integrity regimes in
  the world). Each jurisdiction entry below therefore cites BOTH the
  consumer-product-safety law this fleet's repair-shop catalogs
  already model AND a SEPARATE precious-metal-hallmark-integrity law
  where one actually exists.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries. Japan is
  DELIBERATELY seeded WITHOUT a `:hallmark-*` sub-citation: unlike the
  US/UK/Germany, Japan has no mandatory statutory hallmarking/assay-
  office regime for precious-metal articles in this R0 catalog
  (precious-metal marking in Japan is largely voluntary/JIS-standard-
  based, not a mandatory assay-office system) -- inventing one to make
  coverage look bigger would be the exact fabrication this discipline
  forbids. See `specialtyrepair.governor/hallmark-integrity-
  unconfirmed-violations` for how this is handled as a CONDITIONAL
  check rather than a universal one.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  diagnostic-report/parts-used-documentation/post-repair-safety-test-
  record evidence set (PLUS a hallmark-integrity-record item where a
  jurisdiction actually has such a regime); `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:jurisdiction/assess` proposal can commit.
  `:hallmark-owner-authority` / `:hallmark-legal-basis` /
  `:hallmark-provenance` are the SEPARATE precious-metal-hallmark-
  integrity citation the governor's `hallmark-integrity-unconfirmed?`
  check is grounded in -- present ONLY for jurisdictions that
  actually have such a regime."
  {"JPN" {:name "Japan"
          :owner-authority "経済産業省 (Ministry of Economy, Trade and Industry, METI)"
          :legal-basis "消費生活用製品安全法 (Consumer Product Safety Act)"
          :national-spec "身の回り品修理に関する一般消費生活用製品安全基準"
          :provenance "https://www.meti.go.jp/product_safety/"
          :required-evidence ["故障診断書 (diagnostic report)"
                              "使用部品記録 (parts-used documentation)"
                              "修理後安全試験記録 (post-repair safety-test record)"]}
   "USA" {:name "United States"
          :owner-authority "U.S. Consumer Product Safety Commission (CPSC)"
          :legal-basis "Consumer Product Safety Act (15 U.S.C. §§2051 et seq.)"
          :national-spec "CPSC product-safety standards for personal/household goods"
          :provenance "https://www.cpsc.gov/Regulations-Laws--Standards/Statutes"
          :required-evidence ["Diagnostic report"
                              "Parts-used documentation"
                              "Post-repair safety-test record"
                              "Hallmark-integrity record"]
          :hallmark-owner-authority "Federal Trade Commission (FTC)"
          :hallmark-legal-basis "National Stamping Act (15 U.S.C. §297); FTC Guides for the Jewelry, Precious Metals, and Pewter Industries (16 C.F.R. Part 23)"
          :hallmark-provenance "https://www.ftc.gov/legal-library/browse/rules/jewelry-guides"}
   "GBR" {:name "United Kingdom"
          :owner-authority "Office for Product Safety and Standards (OPSS)"
          :legal-basis "General Product Safety Regulations 2005"
          :national-spec "OPSS product-safety enforcement standards for personal/household goods"
          :provenance "https://www.gov.uk/government/organisations/office-for-product-safety-and-standards"
          :required-evidence ["Diagnostic report"
                              "Parts-used documentation"
                              "Post-repair safety-test record"
                              "Hallmark-integrity record"]
          :hallmark-owner-authority "British Hallmarking Council / UK Assay Offices (London, Birmingham, Sheffield, Edinburgh)"
          :hallmark-legal-basis "Hallmarking Act 1973"
          :hallmark-provenance "https://www.gov.uk/guidance/hallmarking-a-guide-for-buyers-and-sellers"}
   "DEU" {:name "Germany"
          :owner-authority "Marktüberwachungsbehörden der Länder"
          :legal-basis "Produktsicherheitsgesetz (ProdSG)"
          :national-spec "ProdSG Marktüberwachungsanforderungen für Gebrauchsgegenstände"
          :provenance "https://www.baua.de/DE/Themen/Anwendungssichere-Chemikalien-und-Produkte/Produktsicherheit/Produktsicherheit_node.html"
          :required-evidence ["Diagnosebericht (diagnostic report)"
                              "Ersatzteilnachweis (parts-used documentation)"
                              "Sicherheitsprüfungsprotokoll nach Reparatur (post-repair safety-test record)"
                              "Feingehaltsnachweis (hallmark-integrity record)"]
          :hallmark-owner-authority "Eichämter / Edelmetall-Kontrollstellen der Länder"
          :hallmark-legal-basis "Gesetz über den Feingehalt der Gold- und Silberwaren (Feingehaltsgesetz)"
          :hallmark-provenance "https://www.gesetze-im-internet.de/feinges/"}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to complete a
  repair or return an item on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-9529 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `specialtyrepair.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))

(defn hallmark-spec-basis
  "The jurisdiction's precious-metal-hallmark-integrity requirement
  map, or nil -- nil means this jurisdiction has NO formal statutory
  hallmarking/assay-office regime this catalog is aware of (honestly
  true for Japan as of this R0 catalog, unlike the US/UK/Germany)."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:hallmark-owner-authority sb)
      (select-keys sb [:hallmark-owner-authority :hallmark-legal-basis :hallmark-provenance]))))
