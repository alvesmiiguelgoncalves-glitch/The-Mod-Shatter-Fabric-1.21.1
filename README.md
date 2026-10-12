# Shatter (Fabric 1.21.1)

Porte para **Fabric 1.21.1** do mod **Shatter** do iChun (originalmente Forge 1.16.5).
Mod original: <https://ichun.me/mods/shatter/> — © iChun, licença **GNU LGPL v3**.

Quando um mob morre, ele se despedaça: cada caixa do modelo vira um pedaço que gira, voa e vai
sumindo (~5 segundos). É um efeito **só visual e só do cliente** — não precisa instalar no servidor,
não adiciona itens, entidades nem muda o jogo.

## Instalar (só jogar)
1. Fabric Loader 0.16+ e **Fabric API** para o Minecraft 1.21.1.
2. Coloque o `shatter-fabric-1.0.0.jar` na pasta `mods`.

## Gerar o .jar
Precisa de **JDK 21** e internet (o primeiro build baixa o Minecraft e os mapeamentos).

    ./gradlew build          # Windows: gradlew.bat build

O arquivo sai em `build/libs/shatter-fabric-1.0.0.jar` (não use o `-sources`).
Para testar sem instalar: `./gradlew runClient`.

## Configuração
Na primeira execução é criado `config/shatter.json` (mesmas opções do original):

| Opção | Padrão | O que faz |
|---|---|---|
| `enablePlayerShatter` | `true` | Jogadores também se despedaçam |
| `enableBossShatter` | `false` | Bosses (Wither, Ender Dragon) se despedaçam |
| `enableChildShatter` | `false` | Filhotes se despedaçam |

## Ajustes desta versão de correção
- Reduzido o deslocamento horizontal dos fragmentos e o multiplicador que os espalhava longe demais.
- Os fragmentos já começam com transparência leve e desaparecem gradualmente durante a duração do efeito.
- Corrigida a semente aleatória por fragmento para que a posição/rotação não mude aleatoriamente a cada frame.
- O efeito não é aplicado a aranhas, aranhas das cavernas, devastadores, guardiões, guardiões-mestres, slimes, cubos de magma e zumbis gigantes.

**Limitação importante:** os problemas de texturas de profissões/camadas (aldeões, aldeões zumbis, cavalos com acessórios, afogado, stray, vórtice, tatu e outros) ainda precisam de uma correção específica no sistema de renderização. O efeito atual captura o modelo principal e uma única textura; várias dessas aparências são desenhadas por camadas adicionais do renderer. Não quero afirmar que esses bugs foram resolvidos sem conseguir compilar e testar no jogo.

## Diferenças em relação ao original
- Reescrito a partir da leitura do bytecode do original (não havia código-fonte). A física, o tempo
  (espera de 2 ticks, 105 ticks de duração), o fade e o sorteio de movimento de cada pedaço
  seguem os mesmos números e a mesma fórmula.
- Não depende do iChunUtil, e a integração com o mod Morph foi removida.
- Em vez de registrar uma entidade, o efeito é desenhado direto no render do mundo (evita problemas
  de registro no Fabric).
- Bosses: só Wither e Ender Dragon são reconhecidos como boss (bosses de outros mods não).
- Jogador local: em vez de ser removido do mundo, fica invisível até renascer.

## Licença
LGPL-3.0 (veja `LICENSE` e `COPYING`). Se redistribuir, mantenha o crédito ao iChun e o código-fonte.

## Gerar o .jar sem instalar nada (GitHub Actions)
1. Crie um repositório no GitHub e envie todos os arquivos desta pasta (incluindo `.github`).
2. Abra a aba **Actions**, espere o workflow `build` terminar (uns 3–5 min).
3. Entre na execução e baixe o artefato **shatter-fabric-jar**: dentro está o `.jar`.
