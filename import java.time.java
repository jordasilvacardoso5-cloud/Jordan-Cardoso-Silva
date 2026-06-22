import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ===================================================
//  MODELO: Produto
// ===================================================
class Produto {
    private static int contadorId = 1;

    private int id;
    private String nome;
    private String categoria;
    private int quantidade;
    private int minimo;
    private double preco;
    private String unidade;

    public Produto(String nome, String categoria, int quantidade, int minimo, double preco, String unidade) {
        this.id        = contadorId++;
        this.nome      = nome;
        this.categoria = categoria;
        this.quantidade = quantidade;
        this.minimo    = minimo;
        this.preco     = preco;
        this.unidade   = unidade;
    }

    // Status do estoque
    public String getStatus() {
        if (quantidade == 0)          return "ESGOTADO";
        if (quantidade <= minimo)     return "BAIXO";
        return "NORMAL";
    }

    public double getValorTotal() {
        return quantidade * preco;
    }

    // Getters e Setters
    public int    getId()            { return id; }
    public String getNome()          { return nome; }
    public String getCategoria()     { return categoria; }
    public int    getQuantidade()    { return quantidade; }
    public int    getMinimo()        { return minimo; }
    public double getPreco()         { return preco; }
    public String getUnidade()       { return unidade; }

    public void setNome(String nome)             { this.nome = nome; }
    public void setCategoria(String categoria)   { this.categoria = categoria; }
    public void setQuantidade(int quantidade)    { this.quantidade = quantidade; }
    public void setMinimo(int minimo)            { this.minimo = minimo; }
    public void setPreco(double preco)           { this.preco = preco; }
    public void setUnidade(String unidade)       { this.unidade = unidade; }

    @Override
    public String toString() {
        return String.format("[%02d] %-25s | %-12s | Qtd: %3d %-3s | Min: %2d | R$ %6.2f | %s",
                id, nome, categoria, quantidade, unidade, minimo, preco, getStatus());
    }
}

// ===================================================
//  MODELO: Movimentacao
// ===================================================
class Movimentacao {
    private static int contadorId = 1;

    private int    id;
    private String produto;
    private String tipo;       // "ENTRADA" ou "SAIDA"
    private int    quantidade;
    private String obs;
    private String dataHora;

    public Movimentacao(String produto, String tipo, int quantidade, String obs) {
        this.id         = contadorId++;
        this.produto    = produto;
        this.tipo       = tipo;
        this.quantidade = quantidade;
        this.obs        = obs;
        this.dataHora   = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %-8s | %-25s | Qtd: %3d | %s | Obs: %s",
                dataHora, tipo, produto, quantidade,
                tipo.equals("ENTRADA") ? "▲" : "▼",
                obs.isEmpty() ? "-" : obs);
    }
}

// ===================================================
//  SERVIÇO: EstoqueService  (lógica de negócio)
// ===================================================
class EstoqueService {

    private List<Produto>       produtos      = new ArrayList<>();
    private LinkedList<Movimentacao> historico = new LinkedList<>();
    private static final int MAX_HISTORICO    = 20;

    // ---------- CRUD ----------

    public void adicionarProduto(String nome, String categoria, int quantidade,
                                  int minimo, double preco, String unidade) {
        if (nome.isBlank() || quantidade < 0 || preco < 0) {
            System.out.println("  ✗ Dados inválidos. Produto não cadastrado.");
            return;
        }
        Produto p = new Produto(nome, categoria, quantidade, minimo, preco, unidade);
        produtos.add(p);
        System.out.println("  ✓ Produto cadastrado: " + p.getNome());
    }

    public boolean editarProduto(int id, String nome, String categoria,
                                  int quantidade, int minimo, double preco, String unidade) {
        Produto p = buscarPorId(id);
        if (p == null) { System.out.println("  ✗ Produto não encontrado."); return false; }
        p.setNome(nome);
        p.setCategoria(categoria);
        p.setQuantidade(quantidade);
        p.setMinimo(minimo);
        p.setPreco(preco);
        p.setUnidade(unidade);
        System.out.println("  ✓ Produto atualizado: " + nome);
        return true;
    }

    public boolean excluirProduto(int id) {
        Produto p = buscarPorId(id);
        if (p == null) { System.out.println("  ✗ Produto não encontrado."); return false; }
        produtos.remove(p);
        System.out.println("  ✓ Produto removido: " + p.getNome());
        return true;
    }

    // ---------- AJUSTE DE ESTOQUE ----------

    public void ajustarEstoque(int id, String tipo, int quantidade, String obs) {
        if (quantidade <= 0) {
            System.out.println("  ✗ Quantidade deve ser maior que zero.");
            return;
        }
        Produto p = buscarPorId(id);
        if (p == null) { System.out.println("  ✗ Produto não encontrado."); return; }

        String tipoUpper = tipo.toUpperCase();

        if (tipoUpper.equals("ENTRADA")) {
            p.setQuantidade(p.getQuantidade() + quantidade);
        } else if (tipoUpper.equals("SAIDA")) {
            int nova = Math.max(0, p.getQuantidade() - quantidade);
            p.setQuantidade(nova);
        } else {
            System.out.println("  ✗ Tipo inválido. Use ENTRADA ou SAIDA.");
            return;
        }

        registrarMovimentacao(p.getNome(), tipoUpper, quantidade, obs);
        System.out.printf("  ✓ Estoque ajustado. Novo saldo: %d %s%n",
                p.getQuantidade(), p.getUnidade());
    }

    // ---------- FILTROS ----------

    public List<Produto> filtrarPorCategoria(String categoria) {
        if (categoria.equalsIgnoreCase("TODOS")) return new ArrayList<>(produtos);
        return produtos.stream()
                .filter(p -> p.getCategoria().equalsIgnoreCase(categoria))
                .collect(Collectors.toList());
    }

    public List<Produto> buscarPorNome(String termo) {
        return produtos.stream()
                .filter(p -> p.getNome().toLowerCase().contains(termo.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Produto> getAlertas() {
        return produtos.stream()
                .filter(p -> !p.getStatus().equals("NORMAL"))
                .collect(Collectors.toList());
    }

    // ---------- RESUMO ----------

    public void exibirResumo() {
        int totalItens  = produtos.stream().mapToInt(Produto::getQuantidade).sum();
        double valorTotal = produtos.stream().mapToDouble(Produto::getValorTotal).sum();
        long alertas    = produtos.stream().filter(p -> !p.getStatus().equals("NORMAL")).count();

        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║        RESUMO DO ESTOQUE             ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf( "║  Total de produtos : %-16d║%n", produtos.size());
        System.out.printf( "║  Total de itens    : %-16d║%n", totalItens);
        System.out.printf( "║  Alertas           : %-16d║%n", alertas);
        System.out.printf( "║  Valor em estoque  : R$ %-13.2f║%n", valorTotal);
        System.out.println("╚══════════════════════════════════════╝");
    }

    public void exibirProdutos(List<Produto> lista) {
        if (lista.isEmpty()) { System.out.println("  Nenhum produto encontrado."); return; }
        System.out.println("\n" + "─".repeat(85));
        System.out.printf("%-4s %-25s %-13s %-10s %-6s %-10s %s%n",
                "ID", "Nome", "Categoria", "Qtd", "Min", "Preço", "Status");
        System.out.println("─".repeat(85));
        lista.forEach(System.out::println);
        System.out.println("─".repeat(85));
    }

    public void exibirHistorico() {
        if (historico.isEmpty()) { System.out.println("  Nenhuma movimentação registrada."); return; }
        System.out.println("\n── HISTÓRICO DE MOVIMENTAÇÕES ──────────────────────────────");
        historico.forEach(System.out::println);
        System.out.println("─".repeat(60));
    }

    // ---------- AUXILIARES ----------

    public Produto buscarPorId(int id) {
        return produtos.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public List<Produto> getTodosProdutos() { return new ArrayList<>(produtos); }

    private void registrarMovimentacao(String produto, String tipo, int quantidade, String obs) {
        if (historico.size() >= MAX_HISTORICO) historico.removeLast();
        historico.addFirst(new Movimentacao(produto, tipo, quantidade, obs));
    }
}

// ===================================================
//  INTERFACE: Menu interativo no terminal
// ===================================================
class Menu {

    private final EstoqueService servico = new EstoqueService();
    private final Scanner scanner        = new Scanner(System.in);

    public void iniciar() {
        carregarDadosIniciais();

        while (true) {
            exibirMenu();
            int opcao = lerInt("Escolha: ");

            switch (opcao) {
                case 1  -> listarProdutos();
                case 2  -> adicionarProduto();
                case 3  -> editarProduto();
                case 4  -> excluirProduto();
                case 5  -> ajustarEstoque();
                case 6  -> buscarProduto();
                case 7  -> exibirAlertas();
                case 8  -> servico.exibirHistorico();
                case 9  -> servico.exibirResumo();
                case 0  -> { System.out.println("\n  Até logo! 👋"); return; }
                default -> System.out.println("  Opção inválida.");
            }
        }
    }

    private void exibirMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║   🛒  ESTOQUE - MERCADINHO DO BAIRRO  ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. Listar todos os produtos          ║");
        System.out.println("║  2. Adicionar produto                 ║");
        System.out.println("║  3. Editar produto                    ║");
        System.out.println("║  4. Excluir produto                   ║");
        System.out.println("║  5. Entrada / Saída de estoque        ║");
        System.out.println("║  6. Buscar produto por nome           ║");
        System.out.println("║  7. Ver alertas (baixo/esgotado)      ║");
        System.out.println("║  8. Histórico de movimentações        ║");
        System.out.println("║  9. Resumo do estoque                 ║");
        System.out.println("║  0. Sair                              ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    private void listarProdutos() {
        servico.exibirProdutos(servico.getTodosProdutos());
    }

    private void adicionarProduto() {
        System.out.println("\n── NOVO PRODUTO ──");
        String nome      = lerTexto("Nome: ");
        String categoria = lerTexto("Categoria (Mercearia/Laticínios/Bebidas/Hortifrúti/Limpeza/Frios): ");
        int    qtd       = lerInt("Quantidade inicial: ");
        int    minimo    = lerInt("Estoque mínimo: ");
        double preco     = lerDouble("Preço (R$): ");
        String unidade   = lerTexto("Unidade (un/kg/pc/cx/lt): ");
        servico.adicionarProduto(nome, categoria, qtd, minimo, preco, unidade);
    }

    private void editarProduto() {
        System.out.println("\n── EDITAR PRODUTO ──");
        int id = lerInt("ID do produto: ");
        Produto p = servico.buscarPorId(id);
        if (p == null) { System.out.println("  ✗ Produto não encontrado."); return; }
        System.out.println("  Produto atual: " + p);
        String nome      = lerTexto("Novo nome [" + p.getNome() + "]: ");
        String categoria = lerTexto("Nova categoria [" + p.getCategoria() + "]: ");
        int    qtd       = lerInt("Nova quantidade [" + p.getQuantidade() + "]: ");
        int    minimo    = lerInt("Novo mínimo [" + p.getMinimo() + "]: ");
        double preco     = lerDouble("Novo preço [" + p.getPreco() + "]: ");
        String unidade   = lerTexto("Nova unidade [" + p.getUnidade() + "]: ");
        servico.editarProduto(id,
                nome.isBlank()      ? p.getNome()      : nome,
                categoria.isBlank() ? p.getCategoria() : categoria,
                qtd == 0            ? p.getQuantidade() : qtd,
                minimo == 0         ? p.getMinimo()     : minimo,
                preco == 0          ? p.getPreco()      : preco,
                unidade.isBlank()   ? p.getUnidade()    : unidade);
    }

    private void excluirProduto() {
        System.out.println("\n── EXCLUIR PRODUTO ──");
        int id = lerInt("ID do produto: ");
        System.out.print("  Confirmar exclusão? (s/n): ");
        if (scanner.nextLine().equalsIgnoreCase("s")) servico.excluirProduto(id);
        else System.out.println("  Cancelado.");
    }

    private void ajustarEstoque() {
        System.out.println("\n── ENTRADA / SAÍDA ──");
        int    id   = lerInt("ID do produto: ");
        String tipo = lerTexto("Tipo (ENTRADA ou SAIDA): ");
        int    qtd  = lerInt("Quantidade: ");
        String obs  = lerTexto("Observação (opcional): ");
        servico.ajustarEstoque(id, tipo, qtd, obs);
    }

    private void buscarProduto() {
        String termo = lerTexto("\nBuscar por nome: ");
        servico.exibirProdutos(servico.buscarPorNome(termo));
    }

    private void exibirAlertas() {
        List<Produto> alertas = servico.getAlertas();
        System.out.println("\n── ALERTAS ──");
        if (alertas.isEmpty()) System.out.println("  ✓ Nenhum produto com problema de estoque.");
        else servico.exibirProdutos(alertas);
    }

    // ---------- Leitura de dados ----------

    private String lerTexto(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int lerInt(String prompt) {
        System.out.print(prompt);
        try { int v = Integer.parseInt(scanner.nextLine().trim()); return v; }
        catch (NumberFormatException e) { return 0; }
    }

    private double lerDouble(String prompt) {
        System.out.print(prompt);
        try { return Double.parseDouble(scanner.nextLine().trim().replace(",", ".")); }
        catch (NumberFormatException e) { return 0; }
    }

    // ---------- Dados iniciais ----------

    private void carregarDadosIniciais() {
        servico.adicionarProduto("Leite Integral 1L",    "Laticínios",  24, 10,  5.49, "un");
        servico.adicionarProduto("Arroz Branco 5kg",     "Mercearia",    8,  5, 22.90, "pc");
        servico.adicionarProduto("Feijão Carioca 1kg",   "Mercearia",    3,  5,  8.75, "pc");
        servico.adicionarProduto("Refrigerante 2L",      "Bebidas",     30, 12,  9.99, "un");
        servico.adicionarProduto("Sabão em Pó 1kg",      "Limpeza",      2,  4, 14.50, "cx");
        servico.adicionarProduto("Banana Prata (kg)",    "Hortifrúti",  15,  8,  4.20, "kg");
        servico.adicionarProduto("Queijo Mussarela",     "Frios",        5,  3, 38.00, "kg");
        servico.adicionarProduto("Iogurte Natural",      "Laticínios",  12,  6,  3.80, "un");
        System.out.println("\n  ✓ Dados iniciais carregados com sucesso!\n");
    }
}

// ===================================================
//  PONTO DE ENTRADA
// ===================================================
private static EstoqueMercadinho {
    private static void main(String[] args) {
        new Menu().iniciar();
    }
}
