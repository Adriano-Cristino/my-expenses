# MyExpenses - Controle de Despesas Pessoais

> ⚠️ **Status: Em Desenvolvimento** - Este projeto está ativamente em desenvolvimento e algumas funcionalidades podem não estar completamente implementadas.

Aplicativo desktop desenvolvido em Java para controle pessoal de gastos, permitindo que os usuários registrem, organizem e analisem suas despesas de forma prática e eficiente.

## 🚧 Estado Atual do Desenvolvimento

### Funcionalidades Implementadas
- ✅ Sistema de autenticação (Registro e Login)
- ✅ Dashboard básico
- ✅ Estrutura base do projeto
- ✅ Configuração do banco de dados SQLite

### Em Desenvolvimento
- 🔄 Gestão de despesas
- 🔄 Gestão de categorias
- 🔄 Relatórios e gráficos
- 🔄 Exportação de dados

### Próximos Passos
- 📋 Implementar backup e restauração
- 📋 Adicionar temas (claro/escuro)
- 📋 Melhorar a interface do usuário
- 📋 Adicionar testes automatizados

## 🛠️ Tecnologias Utilizadas

- JavaFX 17 - Interface gráfica moderna
- JFoenix - Componentes Material Design
- SQLite - Banco de dados local
- BCrypt - Criptografia de senhas
- Lombok - Redução de boilerplate code
- SLF4J - Logging

## ⚙️ Requisitos

- Java 17 ou superior
- Maven 3.8 ou superior

## 🚀 Executando o Projeto

1. Clone o repositório
```bash
git clone https://github.com/seu-usuario/my-expenses.git
cd my-expenses
```

2. Execute via Maven:
```bash
mvn clean javafx:run
```

## 📁 Estrutura do Projeto

```
src/main/java/com/expenses/
├── config/         # Configurações (DB, Properties)
├── controller/     # Controllers JavaFX
├── dao/           # Acesso a dados
├── model/         # Entidades
├── service/       # Regras de negócio
└── util/          # Classes utilitárias

src/main/resources/
├── css/           # Estilos
├── fxml/          # Layouts
├── images/        # Imagens e ícones
└── sql/           # Scripts SQL
```

## 🤝 Contribuindo

Este projeto está em desenvolvimento ativo e contribuições são bem-vindas! Se você encontrar bugs ou tiver sugestões, por favor:

1. Abra uma issue descrevendo o problema/sugestão
2. Fork o projeto
3. Crie uma branch para sua feature (`git checkout -b feature/MinhaFeature`)
4. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
5. Push para a branch (`git push origin feature/MinhaFeature`)
6. Abra um Pull Request

## 📝 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 📬 Contato

Se você tiver alguma dúvida ou sugestão, sinta-se à vontade para abrir uma issue ou entrar em contato.

---

# [English Version] MyExpenses - Personal Expense Manager

> ⚠️ **Status: Under Development** - This project is actively under development and some features may not be fully implemented.

A desktop application developed in Java for personal expense management, allowing users to record, organize, and analyze their expenses in a practical and efficient way.

## 🚧 Current Development Status

### Implemented Features
- ✅ Authentication system (Registration and Login)
- ✅ Basic Dashboard
- ✅ Project base structure
- ✅ SQLite database configuration

### Under Development
- 🔄 Expense management
- 🔄 Category management
- 🔄 Reports and charts
- 🔄 Data export

### Next Steps
- 📋 Implement backup and restore
- 📋 Add themes (light/dark)
- 📋 Improve user interface
- 📋 Add automated tests

## 🛠️ Technologies Used

- JavaFX 17 - Modern GUI framework
- JFoenix - Material Design components
- SQLite - Local database
- BCrypt - Password encryption
- Lombok - Boilerplate code reduction
- SLF4J - Logging

## ⚙️ Requirements

- Java 17 or higher
- Maven 3.8 or higher

## 🚀 Running the Project

1. Clone the repository
```bash
git clone https://github.com/your-username/my-expenses.git
cd my-expenses
```

2. Run with Maven:
```bash
mvn clean javafx:run
```

## 📁 Project Structure

```
src/main/java/com/expenses/
├── config/         # Configurations (DB, Properties)
├── controller/     # JavaFX Controllers
├── dao/           # Data Access Objects
├── model/         # Entities
├── service/       # Business Logic
└── util/          # Utility Classes

src/main/resources/
├── css/           # Styles
├── fxml/          # Layouts
├── images/        # Images and icons
└── sql/           # SQL Scripts
```

## 🤝 Contributing

This project is under active development and contributions are welcome! If you find bugs or have suggestions, please:

1. Open an issue describing the problem/suggestion
2. Fork the project
3. Create a branch for your feature (`git checkout -b feature/MyFeature`)
4. Commit your changes (`git commit -m 'Add new feature'`)
5. Push to the branch (`git push origin feature/MyFeature`)
6. Open a Pull Request

## 📝 License

This project is under the MIT license. See the [LICENSE](LICENSE) file for more details.

## 📬 Contact

If you have any questions or suggestions, feel free to open an issue or get in touch.
