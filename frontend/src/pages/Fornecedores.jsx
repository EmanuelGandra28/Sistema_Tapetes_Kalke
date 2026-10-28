import CrudPage from '../components/CrudPage';

const fields = [
  { name: 'nomeEmpresa', label: 'Empresa', required: true, full: true },
  { name: 'cnpj', label: 'CNPJ' },
  { name: 'telefone', label: 'Telefone', required: true },
  { name: 'email', label: 'E-mail', type: 'email', full: true },
  { name: 'observacoes', label: 'Observações', full: true }
];
const columns = [{ key: 'nomeEmpresa', label: 'Empresa' }, { key: 'cnpj', label: 'CNPJ' }, { key: 'telefone', label: 'Telefone' }, { key: 'email', label: 'E-mail' }];

// O servidor usa as permissões de produtos para fornecedores.
export default function Fornecedores() {
  return <CrudPage title="Fornecedores" subtitle="Empresas que fornecem mercadoria e matéria-prima." endpoint="/fornecedores" singular="fornecedor" fields={fields} columns={columns}
    permissions={{ create: 'produtos_criar', edit: 'produtos_editar', remove: 'produtos_excluir' }} />;
}
