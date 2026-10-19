# Font licenses

Self-hosted fonts for Open Civic Signal OS, taken from the sibling design harness
`open-beauty-site-quality` (Paper Craft language: Fraunces display + Nunito Sans body).

Both families are licensed under the **SIL Open Font License, Version 1.1** (OFL-1.1),
which permits bundling, embedding and redistribution with software, including modified
(here: subset and format-converted) versions, as long as the license travels with them
and the reserved font names are not used for modified versions sold on their own.

| File | Family / cut | Source | Copyright | License |
| --- | --- | --- | --- | --- |
| `fraunces-soft-560.woff2` | Fraunces, SOFT 100 / WONK 1, weight 560 static instance | `fonts/Fraunces-Soft.ttf` (open-beauty-site-quality) | Copyright 2018 The Fraunces Project Authors (https://github.com/undercasetype/Fraunces) | OFL-1.1 |
| `nunito-sans-400.woff2` | Nunito Sans Regular (400) | `fonts/NunitoSans-Regular.ttf` (open-beauty-site-quality) | Copyright 2016 The Nunito Sans Project Authors (https://github.com/Fonthausen/NunitoSans) | OFL-1.1 |
| `nunito-sans-800.woff2` | Nunito Sans ExtraBold (800) | `fonts/NunitoSans-ExtraBold.ttf` (open-beauty-site-quality) | Copyright 2016 The Nunito Sans Project Authors (https://github.com/Fonthausen/NunitoSans) | OFL-1.1 |

Conversion notes (reproducible):

```
python -m fontTools.subset <src>.ttf --output-file=<dst>.woff2 --flavor=woff2 \
  --unicodes=U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,U+2000-206F,U+2074,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD \
  --layout-features=kern,liga,calt --name-IDs=1,2,3,4,5,6,13,14
```

Only the Latin subset (covering EN + ES, including accented characters and Spanish
punctuation) is shipped: 3 files, ~39 KB total. License texts:
`OFL-fraunces.txt`, `OFL-nunitosans.txt` in the harness `fonts/` folder.
